package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryRunner;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.ExportResultsIterator;
import org.intermine.web.logic.results.ResultElement;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.template.TemplateQuery;

/**
 * Executes path query and returns results in form suitable for export or web
 * services.
 *
 * @author Jakub Kulaviak
 */
public class PathQueryExecutor
{

    private static final int DEFAULT_BATCH_SIZE = 5000;

    private Map<String, InterMineBag> allBags;

    private BagQueryRunner runner;

    private ObjectStore os;

    private int batchSize = DEFAULT_BATCH_SIZE;

    /**
     * Sets batch size.
     *
     * @param size batch size
     */
    public void setBatchSize(int size) {
        this.batchSize = size;
    }

    /**
     * Constructor with necessary objects.
     *
     * @param os the ObjectStore to run the query in
     * @param classKeys key fields for classes in the data model
     * @param bagQueryConfig bag queries to run when interpreting LOOKUP constraints
     * @param profile the user executing the query - for access to saved lists
     * @param conversionTemplates templates used for converting bag query results between types
     * @param searchRepository global search repository to fetch saved bags from
     */
    public PathQueryExecutor(ObjectStore os,
            Map<String, List<FieldDescriptor>> classKeys,
            BagQueryConfig bagQueryConfig, Profile profile,
            List<TemplateQuery> conversionTemplates,
            SearchRepository searchRepository) {
        this.os = os;
        this.runner = new BagQueryRunner(os, classKeys, bagQueryConfig,
                conversionTemplates);
        this.allBags = WebUtil.getAllBags(profile.getSavedBags(),
                searchRepository);
    }

    /**
     * Executes object store query and returns results as iterator over rows.
     * Every row is a list of result elements.
     *
     * @param pathQuery path query to be executed
     * @return results
     */
    public ExportResultsIterator execute(PathQuery pathQuery) {
        try {
            return new ExportResultsIterator(os, pathQuery, allBags, runner,
                    batchSize);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(
                    "Creating export results iterator failed", e);
        }
    }

    /**
     * Executes object store query and returns results as iterator over rows.
     * Every row is a list of result elements.
     *
     * @param pathQuery path query to be executed
     * @param start index of first result which will be retrieved. It can be very slow, it fetches
     * results from database from index 0 and just throws away all before start index.
     * @param limit maximum number of results
     * @return results
     */
    public ExportResultsIterator execute(PathQuery pathQuery, final int start,
            final int limit) {
        try {
            return new ResultIterator(os, pathQuery, allBags, runner, batchSize, start, limit);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(
                    "Creating export results iterator failed", e);
        }
    }
}

/**
 * Class adapting ExportResultsIterator to be able to get results only in specified range
 * but is very slow, it just throws away all results before the start index.
 *
 * @author Jakub Kulaviak
 */
class ResultIterator extends ExportResultsIterator
{
    private int counter = 0;

    private int limit;

    private int start;

    /**
     * Constructor for ExportResultsIterator. This creates a new instance from the given
     * ObjectStore, PathQuery, and other necessary objects.
     *
     * @param os an ObjectStore that the query will be run on
     * @param pq a PathQuery to run
     * @param savedBags a Map of the bags that the query may have used
     * @param bagQueryRunner a BagQueryRunner for any LOOKUP constraints
     * @param batchSize the batch size for the results
     * @param start start index from which retrieve results
     * @param limit maximum number retrieved results
     * @throws ObjectStoreException if something goes wrong executing the query
     */
    public ResultIterator(ObjectStore os, PathQuery pq, Map savedBags,
            BagQueryRunner bagQueryRunner, int batchSize, int start,
            int limit) throws ObjectStoreException {
        super(os, pq, savedBags, bagQueryRunner, batchSize);
        this.limit = limit;
        this.start = start;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        // throw away results before start index
        while (counter < start) {
            if (super.hasNext()) {
                next();
            } else {
                return false;
            }
        }

        if (counter >= (limit + start)) {
            return false;
        } else {
            return super.hasNext();
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<ResultElement> next() {
        List<ResultElement> ret = (List<ResultElement>) super.next();
        counter++;
        return ret;
    }
}
