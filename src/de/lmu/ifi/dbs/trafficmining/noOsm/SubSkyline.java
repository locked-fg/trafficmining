package de.lmu.ifi.dbs.trafficmining.noOsm;

import de.lmu.ifi.dbs.trafficmining.noOsm.WeightedNode2D;
import de.lmu.ifi.dbs.trafficmining.noOsm.WeightedLink;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Deprecated
public class SubSkyline {

    WeightedNode2D end;
    float mintodest;
    Approximation minDist;
    float preference;
    LinkedList<ComplexPath> skyline;

    public SubSkyline(ComplexPath first, float[] pref, Approximation mD, WeightedNode2D dest) {
        minDist = mD;
        if(first.getLast().getRefDist()!= null && dest.getRefDist()!= null)
        	mintodest = minDist.estimate(first.getLast(), dest);
        else
        	mintodest = 0;
        preference = first.prefVal(pref) + mintodest;
        skyline = new LinkedList<ComplexPath>();
        skyline.add(first);
        end = first.getLast();
    }

    public float getPreference() {
        return preference;
    }

    public WeightedNode2D getEnd() {
        return end;
    }

    public List<ComplexPath> getSkyline() {
        return skyline;
    }

    public void update(ComplexPath p, float[] pref) {
        Iterator<ComplexPath> skyIter = skyline.iterator();
        LinkedList<ComplexPath> delList = new LinkedList();
        while (skyIter.hasNext()) {
            ComplexPath skyPath = skyIter.next();
            if (skyPath.isSame(p)) {
                return;
            }
            float result = skyPath.dominates(p, end, minDist);
            if (result == 1) {
                return;
            } else {
                if (result == -1) {
                    delList.add(skyPath);
                }
            }
        }
        Iterator<ComplexPath> pIter = delList.iterator();
        while (pIter.hasNext()) {
            skyline.remove(pIter.next());
        }
        skyline.add(p);
        preference = Math.min(preference, p.prefVal(pref) + mintodest);
    }

    public List<ComplexPath> extend(List<WeightedLink> links) {
        LinkedList<ComplexPath> result = new LinkedList();
        Iterator<ComplexPath> skyIter = skyline.iterator();
        while (skyIter.hasNext()) {
            ComplexPath p = skyIter.next();
            if (p.isProcessed()) {
                continue;
            }
            Iterator<WeightedLink> lIter = links.iterator();

            while (lIter.hasNext()) {
                ComplexPath newPath = new ComplexPath(p,lIter.next());
                //if(newPath != null)
                result.add(newPath);
            }
            p.setProcessed();
            //p.prev = null;
        }
        return result;
    }
}
