package de.lmu.ifi.dbs.paros.noOsm;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.paros.graph.Path;

@Deprecated
public class ComplexPath extends Path<ComplexPath, WeightedNode2D, WeightedLink> {

    float[] cost;
    private boolean processed = false;
 
    public ComplexPath(WeightedNode2D m, WeightedNode2D n, float[] c) {
        super(m, n, 1, null);
        cost = c;
    }
    
    
    public float[] getCost(){
    	return cost;
    }
    
    public ComplexPath(ComplexPath p, WeightedNode2D n, float[] c) {
        super(p.getFirst(), n, p.getLength() + 1, p);
        cost = new float[p.cost.length];
        for (int d = 0; d < cost.length; d++) {
            cost[d] = p.cost[d] + c[d];
        }
    }

    public ComplexPath(WeightedNode2D n, WeightedLink l) {
        super(n, l);
        cost = l.getWeights();
    }

    public ComplexPath(ComplexPath p, WeightedLink l) {
        super(p,l);
        cost = new float[p.cost.length];
        for (int d = 0; d < cost.length; d++) {
            cost[d] = p.cost[d] + l.getWeights()[d];
        }
    }

    public float prefVal(float[] w) {
        float result = 0;
        for (int d = 0; d < w.length; d++) {
            result += w[d] * cost[d];
        }
        return result;
    }

    public float dominates(ComplexPath p2, WeightedNode2D dest, Approximation minDist) {
        WeightedNode2D end = getLast();
        if (end == dest) {
            if (p2.getLast() == dest) {
                boolean pbetterp2 = false;
                boolean p2betterp = false;
                //boolean pequalp2 = true;
                for (int d = 0; d < p2.cost.length; d++) {
                    if (cost[d] - p2.cost[d] > 0.0000001) {
                        p2betterp = true;
                        //pequalp2 = false;
                    }
                    if (p2.cost[d] - cost[d] > 0.0000001) {
                        pbetterp2 = true;
                        //pequalp2 = false;
                    }
                }
                if (pbetterp2 && !p2betterp) {
                    return 1;
                }
                if ((p2betterp && !pbetterp2)) {
                    return -1;
                }
//				if(pequalp2){
//					if(p2.contains("approx")&& history.contains("approx")){
//						if(p2.getLength() > getLength())
//							return-1;
//						else
//							return 1;
//					}else{
//						if(p2.history.contains("approx"))
//							return 1;
//						if(history.contains("approx"))
//							return -1;
//					}
//				}
            } else {
                boolean pbetterp2 = false;
                //boolean pequalp2 = true;
                for (int d = 0; d < p2.cost.length; d++) {
                    float min = p2.cost[d] + minDist.estimateX(p2.getLast(), dest, d);
                    if (cost[d] - min > 0.0000001) {
                        return 0;
                    }
                    if (min - cost[d] > 0.0000001) {
                        pbetterp2 = true;
                    }
//					if(pequalp2 && min != cost[d]) {
//						pequalp2 = false;						
//					}
                }
                if (pbetterp2) {
                    return 1;
                }
//				if(pequalp2){
//					if(p2.history.contains("approx")&& history.contains("approx")){
//						if(p2.getLength()>getLength())
//							return-1;
//						else
//							return 1;
//					}else{
//						if(p2.history.contains("approx"))
//							return 1;
//						if(history.contains("approx"))
//							return -1;
//					}
//				}
            }
        }
        return 0;
    }
    
   
//	/**
//	 *This method tests whether the attribute path (AP) improves the caller (CP)
//	 * path significantly. The CP improves AP significantly if CP is better is comparable (+delta)
//	 * in each dimension.
//	 * AP dominates CP if it is delta-comparable w.r.t. all dimension and is delta-better in at least
//	 * one dimension. 
//	 * If AP is delta better in one Dimension and CP is better in another one, the method returns
//	 * 0 to signal the draw. 
//	 * 
//	 * @param p2
//	 * @param dest
//	 * @param minDist
//	 * @return
//	 */
//	
//	
//	public double improves(ComplexPath p2, WeightedNode2D dest, Approximation minDist) {
//		double[] delta = new double[cost.length];
//		for (int d = 0; d < p2.cost.length; d++)
//			delta[d] = 0.1 * cost[d];
//		if (getLast() == dest) {
//			if (p2.getLast() == dest) {
//				boolean pbetterp2 = false;
//				boolean p2betterp = false;
//				boolean pequalp2= true;
//				for (int d = 0; d < p2.cost.length; d++) {
//					if ( cost[d]-p2.cost[d]> delta[d] ) {
//						p2betterp = true;
//						pequalp2 = false;
//					}
//					if (p2.cost[d] - cost[d]>delta[d]) {
//						pbetterp2 = true;
//						pequalp2 = false;
//					}					
//				}
//				if (pbetterp2 && !p2betterp)
//					return 1;
//				if (p2betterp && !pbetterp2)
//					return -1;
//				if(pequalp2){
//					if(p2.history.contains("approx")&& history.contains("approx")){
//						if(p2.getLength()> getLength())
//							return-1;
//						else
//							return 1;
//					}else{
//						if(p2.history.contains("approx"))
//							return 1;
//						if(history.contains("approx"))
//							return -1;
//					}					
//				}
//				if(pbetterp2 && p2betterp)
//					return 0;
//				else
//					return 1;
//			} else {
//				boolean pbetterp2 = false;
//				boolean pequalp2 = true;
//				for (int d = 0; d < p2.cost.length; d++) {
//					double min = p2.cost[d]+ minDist.estimateX(p2.end, dest, d);
//					if ( cost[d]-min >delta[d]) {
//						return 0;						
//					}
//					if (min - cost[d]>delta[d]) {
//						pbetterp2 = true;						
//					}
//					if(pequalp2 && min != cost[d]) {
//						pequalp2 = false;						
//					}
//				}
//				if (pbetterp2)
//					return 1;
//				if(pequalp2){
//					if(p2.history.contains("approx")&& history.contains("approx")){
//						if(p2.history.length()>history.length())
//							return-1;
//						else
//							return 1;
//					}else{
//						if(p2.history.contains("approx"))
//							return 1;
//						if(history.contains("approx"))
//							return -1;
//					}
//				}
//			}
//		}
//		return 0;
//	}

//	public double dominatesMinMax(ComplexPath p2, WeightedNode2D dest, Approximation minDist) {
//		double[] pMindist = null;
//		double[] p2Mindist = null;
//		double[][] pMaxdist = null;
//		double[][] p2Maxdist = null;
//		if (getLast() == dest) {
//			pMaxdist = new double[1][];
//			pMaxdist[0] = cost;
//			pMindist = cost;
//		} else {
//			pMindist = calcMindist(this, dest, minDist);
//			pMaxdist = calcMaxDist(this, dest);
//		}
//
//		if (p2.getLast() == dest) {
//			p2Maxdist = new double[1][];
//			p2Maxdist[0] = p2.cost;
//			p2Mindist = p2.cost;
//		} else {
//			p2Mindist = calcMindist(p2, dest, minDist);
//			p2Maxdist = calcMaxDist(p2, dest);
//		}
//		// test if p dominates p2
//		if (testDomination(p2Mindist, pMaxdist))
//			return 1;
//		// test if p2 dominates p
//		if (testDomination(pMindist, p2Maxdist))
//			return -1;
//		return 0;
//	}
//	public double dominates2(ComplexPath p2, WeightedNode2D dest, Approximation minDist) {
//		if (getLast() == dest) {
//			// false caller bereits exakte Lösung, verwende dominates
//			double d = dominates(p2, dest, minDist);
//			if (d != 0)
//				return d;
//			else {
//				// checke ob p2 Caller p prunen kann
//				boolean dominates = false;
//				for (int r = 0; r < p2.end.refPaths[0].length; r++) {
//					boolean p2betterp = false;
//					boolean p2equalp = true;
//					for (int p = 0; p < p2.cost.length; p++) {
//						double pMindist = cost[p]
//								+ minDist.estimateX(end, dest, p);
//						double p2MaxDist = p2.cost[p] + p2.end.refPaths[p][r]
//								+ dest.refPaths[p][r];
//						if (  p2MaxDist- pMindist >0.0000000001) {
//							p2equalp = false;
//							break;
//						}
//						if (pMindist - p2MaxDist >0.0000000001) {
//							p2betterp = true;
//						}
//					}
//					if (p2equalp && p2betterp) {
//						dominates = true;
//						break;
//					}
//				}
//				if (dominates)
//					return -1;
//			}
//		} else {
//			if (p2.end == dest) {
//				// caller ist keine Lösung aber p2
//				double d = p2.dominates(this, dest, minDist);
//				if (d != 0)
//					return -d;
//			} else {
//				// beides ist keine Lösung
//				boolean dominates = false;
//				for (int r = 0; r < p2.end.refPaths[0].length; r++) {
//					boolean p2betterp = false;
//					boolean p2equalp = true;
//					for (int p = 0; p < p2.cost.length; p++) {
//						double pMindist = cost[p]
//								+ minDist.estimateX(end, dest, p);
//						double p2MaxDist = p2.cost[p] + p2.end.refPaths[p][r]
//								+ dest.refPaths[p][r];
//						if ( p2MaxDist- pMindist >0.0000000001) {
//							p2equalp = false;
//							break;
//						}
//						if (pMindist - p2MaxDist >0.0000000001) {
//							p2betterp = true;
//						}
//					}
//					if (p2equalp && p2betterp) {
//						dominates = true;
//						break;
//					}
//				}
//				if (dominates)
//					return -1;
//				dominates = false;
//				for (int r = 0; r < end.refPaths[0].length; r++) {
//					boolean pbetterp2 = false;
//					boolean pequalp2 = true;
//					for (int p = 0; p < cost.length; p++) {
//						double p2Mindist = p2.cost[p]
//								+ minDist.estimateX(p2.end, dest, p);
//						double pMaxDist = cost[p] + end.refPaths[p][r]
//								+ dest.refPaths[p][r];
//						if (pMaxDist - p2Mindist > 0.0000000001) {
//							pequalp2 = false;
//							break;
//						}
//						if (p2Mindist - pMaxDist>0.0000000001) {
//							pbetterp2 = true;
//						}
//					}
//					if (pequalp2 && pbetterp2) {
//						dominates = true;
//						break;
//					}
//				}
//				if (dominates)
//					return 1;
//			}
//		}
//		return 0;
//	}
    private float[] calcMindist(ComplexPath p2, WeightedNode2D dest, Approximation minDist) {
        float[] p2Mindist;
        p2Mindist = new float[p2.cost.length];
        for (int p = 0; p < p2.cost.length; p++) {
            p2Mindist[p] = cost[p] + minDist.estimateX(p2.getLast(), dest, p);
        }
        return p2Mindist;
    }
    
    /**
     * Reverses the path
     * @param n
     * @return
     */
    public ComplexPath reverse() {
    	List<ComplexPath> subPaths = new ArrayList<ComplexPath>(this.getLength());
        ComplexPath p = this.getParent();
        while (p != null) {        	
        	subPaths.add(p);
        	p = p.getParent();
        }
        ComplexPath result = null;
        float[] loc_costs = new float[this.cost.length];
        System.arraycopy(this.cost,0, loc_costs, 0, this.cost.length);      
        for(ComplexPath aktPath : subPaths){
        	if(result==null){
        		float[] aktCosts = new float[this.cost.length];
        		for(int d = 0; d < aktPath.cost.length; d++)
        			aktCosts[d] = loc_costs[d]- aktPath.cost[d];
        		result = new ComplexPath(this.getLast(),aktPath.getLast(), aktCosts);
        		loc_costs = aktPath.cost;        	
        	}
        	else{
        		float[] aktCosts = new float[this.cost.length];
        		for(int d = 0; d < aktPath.cost.length; d++)
        			aktCosts[d] = loc_costs[d]- aktPath.cost[d];
        		result = new ComplexPath(result , aktPath.getLast(),aktCosts);
        		loc_costs =aktPath.cost;
        		
        	}        		
        }
        if(result!=null){        	  	
        	result = new ComplexPath(result , this.getFirst(),loc_costs);
        }
        else
        	result = new ComplexPath(this.getLast() , this.getFirst(),this.cost);
        return result;
    }
    
    /**
     * Appends one path to another
     * @param n
     * @return
     */
    public ComplexPath append(ComplexPath end){    	
    	if(this.getLast().equals(end.getFirst()))
    		end = end.reverse();
    	if(!this.getLast().equals(end.getLast()))
    		throw new IllegalArgumentException("Paths not connected !");  
    	ComplexPath aktApp = end.getParent();
    	ComplexPath temp = this; 
    	float[] old = end.cost;
    	while(aktApp !=null){
    		float[] aktCost = new float[cost.length];
    		for(int d = 0; d < aktCost.length; d++)
    			aktCost[d] = old[d]-aktApp.cost[d];
    		temp = new ComplexPath(temp, aktApp.getLast(), aktCost);
    		old = aktApp.cost;
    		aktApp = aktApp.getParent();    		
    	}    	
    	temp = new ComplexPath(temp, end.getFirst(),old );    	
    	return  temp;
    }
//	private double[][] calcMaxDist(ComplexPath path, WeightedNode2D dest) {
//		double[][] pMaxdist;
//		pMaxdist = new double[path.end.refPaths[0].length][path.cost.length];
//		for (int p = 0; p < cost.length; p++) {
//			for (int r = 0; r < pMaxdist.length; r++) {
//				pMaxdist[r][p] = path.cost[p] + path.end.refPaths[p][r]
//						+ dest.refPaths[p][r] + EPSILON;
//			}
//		}
//		return pMaxdist;
//	}
//    private boolean testDomination(float[] pMindist, float[][] p2Maxdist) {
//        boolean dominates = false;
//        for (int r = 0; r < p2Maxdist.length; r++) {
//            boolean p2betterp = false;
//            boolean p2equalp = true;
//            for (int p = 0; p < p2Maxdist[r].length; p++) {
//                if (pMindist[p] < p2Maxdist[r][p]) {
//                    p2equalp = false;
//                    break;
//                }
//                if (pMindist[p] > p2Maxdist[r][p]) {
//                    p2betterp = true;
//                }
//            }
//            if (p2equalp && p2betterp) {
//                dominates = true;
//                break;
//            }
//        }
//        return dominates;
//    }

    public boolean isSame(ComplexPath p) {
        if (getLength() != p.getLength() || getLast() != p.getLast() || getFirst() != p.getFirst()) {
            return false;
        }
        for (int x = 0; x < p.cost.length; x++) {
            if (p.cost[x] != cost[x]) {
                return false;
            }
        }
        return true;
    }

    public Comparable generateKey() {
        return null;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed() {
        processed = true;
    }

    public void printPath() {
//        String line = "end";
//        ComplexPath p = this;
//        while (p.getLength() > 1) {
//            line = p.getLast().getName() + " - " + line;
//            p = p.getParent();
//        }
//        line = p.getLast().getName() + " - " + line;
//        line = p.getFirst().getName() + " - " + line;
//        System.out.println(line);
        System.out.println(toString());
    }
}
