
package de.lmu.ifi.dbs.trafficmining.ui.result;

import java.util.Comparator;

public class ResultTableColumnSorter implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {
        double d1 = Double.parseDouble(o1.toString().split(" ")[0]);
        double d2 = Double.parseDouble(o2.toString().split(" ")[0]);
        return Double.compare(d1, d2);
    }
}