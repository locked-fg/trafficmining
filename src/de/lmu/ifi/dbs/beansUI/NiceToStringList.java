package de.lmu.ifi.dbs.beansUI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A simple ArrayList that just overrides the toString() method.<br>
 * The clas is used in the beans config dialogue for nicer
 * representations of list options.
 *
 * Before: [[A], [B]]<br>
 * Now:    A, B
 * @author graf
 */
class NiceToStringList<E> extends ArrayList<E> {

    NiceToStringList(List list) {
        super(list);
    }

    NiceToStringList() {
    }

    /**
     * toString method body derived from AbstractCollection
     * @return
     */
    @Override
    public String toString() {
        Iterator<E> i = iterator();
        if (!i.hasNext()) {
            //	    return "[]";
            return "";
        }

        StringBuilder sb = new StringBuilder();
//	sb.append('[');
        for (;;) {
            E e = i.next();
            sb.append(e == this ? "(this Collection)" : e);
            if (!i.hasNext()) {
                // return sb.append(']').toString();
                return sb.toString();
            }
            sb.append(", ");
        }
    }
}
