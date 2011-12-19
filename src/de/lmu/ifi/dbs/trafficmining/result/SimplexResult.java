package de.lmu.ifi.dbs.trafficmining.result;

///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package de.lmu.ifi.dbs.paros.result;
//
//import de.lmu.ifi.dbs.paros.graph.Path;
//import java.util.Arrays;
//
///**
// *
// * @author wombat
// */
//public class SimplexResult extends AbstractResult {
//
//    /**
//     * @param path
//     * @param weight Weight might be cost according to time or length
//     */
//    public void addResult(Path path, double weight) {
//        addResult(path, new double[]{weight});
//    }
//
//    @Override
//    public void setUnits(String... unitStrings) {
//        if (unitStrings != null && unitStrings.length <= 0) {
//            throw new IllegalArgumentException("no units?");
//        }
//        units.clear();
//        units.addAll(Arrays.asList(unitStrings));
//    }
//
//    @Override
//    public void setAttributes(String... attribStrings) {
//        if (attribStrings != null && attribStrings.length <= 0) {
//            throw new IllegalArgumentException("no attribs?");
//        }
//        attribs.clear();
//        attribs.addAll(Arrays.asList(attribStrings));
//    }
//}
