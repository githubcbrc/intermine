package org.flymine.objectstore.query.presentation;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.Iterator;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.flymine.objectstore.query.*;
import org.flymine.FlyMineException;

/**
 * Static methods to assist with query generation from front end.
 *
 * @author Richard Smith
 */
public class QueryCreator
{
    static final String DATE_FORMAT = "dd/MM/yyyy";

    /**
     * Add a QueryClass to the from list of query and add constraints
     * generated from a map of fields/values.
     *
     * @param q a query to add QueryClass and constraints to
     * @param clsName name of class to add to query
     * @param fields map of fieldname/value to build constraints from
     * @throws FlyMineException if an error occurs
     * @throws NullPointerException if any of the parameters are null
     */
    public static void addToQuery(Query q, String clsName, Map fields) throws FlyMineException,
                                                                              NullPointerException {
        if (q == null) {
            throw new NullPointerException("Query parameter is null");
        } else if (clsName == null || clsName.equals("")) {
            throw new NullPointerException("clsName parameter is null");
        } else if (fields == null) {
            throw new NullPointerException("fields parameter is null");
        }

        try {
            QueryClass qc = new QueryClass(Class.forName(clsName));
            q.addFrom(qc);
            addConstraint(q, generateConstraints(qc, fields));
        } catch (Exception e) {
            throw new FlyMineException("Problem occurred adding class (" + clsName
                                + ") to query", e);
        }
    }

    /**
     * Generate ConstraintSet of SimpleConstraints for a QueryClass from a map of field/value pairs
     *
     * @param qc QueryClass to constrain
     * @param fields map of fieldname/value
     * @return a populated ConstraintSet
     * @throws Exception if it goes wrong
     */
    protected static ConstraintSet generateConstraints(QueryClass qc, Map fields)
        throws Exception {
        ConstraintSet constraints = new ConstraintSet(ConstraintSet.AND);
        Iterator iter = fields.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry fieldEntry = (Map.Entry) iter.next();
            if (!(((String) fieldEntry.getValue()).equals(""))) {
                QueryField qf = new QueryField(qc, (String) fieldEntry.getKey());
                QueryValue qv = createQueryValue(qf.getType(), (String) fieldEntry.getValue());
                SimpleConstraint sc = new SimpleConstraint(qf, SimpleConstraint.EQUALS, qv);
                constraints.addConstraint(sc);
            }
        }
        return constraints;
    }

    /**
     * Adds the constraints in a ConstraintSet to those present in a Query
     *
     * @param q the query in question
     * @param constraints the new constraints
     */
    protected static void addConstraint(Query q, ConstraintSet constraints) {
        if (constraints.getConstraints().size() > 0) {
            Constraint c = q.getConstraint();
            if (c == null) {
                q.setConstraint(constraints);
            }  else if (c instanceof ConstraintSet) {
                ((ConstraintSet) c).addConstraint(constraints);
            }  else { // any other type of constraint
                constraints.addConstraint(c);
            q.setConstraint(constraints);
            }
        }
    }

    /**
     * Create a QueryValue by parsing a string for the appropriate class type
     * (common java.lang complex types and java.util.Date supported).
     *
     * @param type java type of the QueryValue to be created
     * @param value string to be parsed for value
     * @return a new QueryValue
     * @throws IllegalArgumentException if type is not supported
     * @throws ParseException if an error occurs parsing date string
     */
    protected static QueryValue createQueryValue(Class type, String value)
        throws IllegalArgumentException, ParseException {
        QueryValue qv = null;
        if (type.equals(Integer.class)) {
            qv = new QueryValue(new Integer(Integer.parseInt(value)));
        } else if (type.equals(Float.class)) {
            qv = new QueryValue(new Float(Float.parseFloat(value)));
        } else if (type.equals(Double.class)) {
            qv = new QueryValue(new Double(Double.parseDouble(value)));
        } else if (type.equals(Long.class)) {
            qv = new QueryValue(new Long(Long.parseLong(value)));
        } else if (type.equals(Short.class)) {
            qv = new QueryValue(new Short(Short.parseShort(value)));
        } else if (type.equals(Boolean.class)) {
            qv = new QueryValue(Boolean.valueOf(value));
        } else if (type.equals(Date.class)) {
            qv = new QueryValue(new SimpleDateFormat(DATE_FORMAT).parse(value));
        } else if (type.equals(String.class)) {
            qv = new QueryValue(value);
        } else {
            throw new IllegalArgumentException("Invalid type for QueryValue: " + type);
        }
        return qv;
    }
}
