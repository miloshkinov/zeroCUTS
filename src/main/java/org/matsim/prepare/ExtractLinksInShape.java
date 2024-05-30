/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
  
/**
 * 
 */
package org.matsim.prepare;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup.CompressionType;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

/**
 * @author kturner
 *
 */
public class ExtractLinksInShape {
	
	private static final String INPUT_DIR = "original-input-data/berlin-shp/" ;	

	private static final String ZONESHAPEFILE = INPUT_DIR + "/berlin.shp";
	private static final String NETWORKSHAPEFILE_LINKS =  "nw_berlin_links.shp";
	private static final String NETWORKSHAPEFILE_NODES =  "nw_berlin_nodes.shp";
	
	//Korrekturen die noch in Liste aufgeommen werden sollen oder gerade nicht. (Bitte als String einfügen
	private static final ArrayList<String> removeLinks = new ArrayList<String>(Arrays.asList());
	private static final ArrayList<String> addLinks =  new ArrayList<String>(Arrays.asList());

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//Network-Stuff
		String inputFileNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/"
				   + "output-berlin-v5.2-1pct/berlin-v5.2-1pct.output_network.xml.gz" ;
 		final Config config = ConfigUtils.createConfig();
 		config.controller().setOutputDirectory("output/shape/");
 		// (the directory structure is needed for output, which is before the controler starts.  Maybe there is a better alternative ...)
 		config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		new OutputDirectoryHierarchy( config.controller().getOutputDirectory(), config.controller().getOverwriteFileSetting(), CompressionType.gzip ) ;
		config.controller().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles );
		
		config.network().setInputFile( inputFileNetwork );
		config.global().setCoordinateSystem( "GK4" ); 
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		Network network = scenario.getNetwork() ;

		convertNet2Shape(network, config.controller().getOutputDirectory()); //Step 1: Netzwerk zusammenbringen

		Collection<SimpleFeature>  zoneFeatures = new ShapeFileReader().readFileAndInitialize(ZONESHAPEFILE);
		Collection<SimpleFeature>  linkFeatures = new ShapeFileReader().readFileAndInitialize(config.controller().getOutputDirectory() + NETWORKSHAPEFILE_LINKS);
		Collection<SimpleFeature>  nodeFeatures = new ShapeFileReader().readFileAndInitialize(config.controller().getOutputDirectory() + NETWORKSHAPEFILE_NODES);

		ArrayList<String> nodesInZone = calcNodesInZone(zoneFeatures, nodeFeatures);
		extractLinksInArea(linkFeatures, nodesInZone, zoneFeatures , config.controller().getOutputDirectory());

		System.out.println("### ENDE ###");
	}
	
	private static void convertNet2Shape(Network network, String outputDir){

		CoordinateReferenceSystem crs = MGC.getCRS("EPSG:31468"); 
//		network = convertCoordinates(network, "EPSG:31468", crs.getCoordinateSystem().toString());
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder().
				setCrs(crs).
				setName("link").
				addAttribute("ID", String.class).
				addAttribute("fromID", String.class).
				addAttribute("toID", String.class).
				addAttribute("length", Double.class).
				addAttribute("type", String.class).
				addAttribute("capacity", Double.class).
				addAttribute("freespeed", Double.class).
				create();

		for (Link link : network.getLinks().values()) {
			Coordinate fromNodeCoordinate = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
			Coordinate toNodeCoordinate = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
			Coordinate linkCoordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
			SimpleFeature ft = linkFactory.createPolyline(new Coordinate [] {fromNodeCoordinate, linkCoordinate, toNodeCoordinate},
					new Object [] {link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(), 
									link.getLength(), NetworkUtils.getType(((Link)link)), link.getCapacity(), link.getFreespeed()}, null);
			features.add(ft);
		}   
		ShapeFileWriter.writeGeometries(features, outputDir + NETWORKSHAPEFILE_LINKS);


		features.clear();

		PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
				setCrs(crs).
				setName("nodes").
				addAttribute("ID", String.class).
				create();

		for (Node node : network.getNodes().values()) {
			SimpleFeature ft = nodeFactory.createPoint(node.getCoord(), new Object[] {node.getId().toString()}, null);
			features.add(ft);
		}
		ShapeFileWriter.writeGeometries(features, outputDir + NETWORKSHAPEFILE_NODES);
	}

	// Extract all Features of network shape which are within the ZoneShape, write their IDs into a .txt-File 
	// which is designed in a way, that it can get copied easily to a tollFill and create a .shp-File with this Features
	private static void extractLinksInArea(Collection<SimpleFeature> linkFeatures, 
			ArrayList<String> nodesInZone, Collection<SimpleFeature> zoneFeatures, String outputDir) {

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		FileWriter writer;
		try {
			writer = new FileWriter(new File(outputDir + "linkIDsInZone.txt")); //- falls die Datei bereits existiert wird diese überschrieben
		
			for (SimpleFeature zoneFeature : zoneFeatures){
				for(SimpleFeature linkFeature : linkFeatures){ 
					Geometry zoneGeometry = (Geometry) zoneFeature.getDefaultGeometry();
					Geometry networkGeometry = (Geometry) linkFeature.getDefaultGeometry();
					if( ( zoneGeometry.contains(networkGeometry) 		 // Link innerhalb der Zone
							|| (zoneGeometry.crosses(networkGeometry) && (nodesInZone.contains(linkFeature.getAttribute("toID"))) )	 //TODO: Wollen wir das haben? Link kreuzt Zonengrenze von außen nach innen.
							|| addLinks.contains(linkFeature.getAttribute("ID")) ) //Link soll manuell hinzugefügt werden
							&& !removeLinks.contains(linkFeature.getAttribute("ID")) )  //Link soll NICHT aufgenommen werden
					{
						features.add(linkFeature);
						// Text wird in den Stream geschrieben
						writer.write(linkFeature.getAttribute("ID").toString());
						writer.write(System.getProperty("line.separator"));
					}
				}
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Datei geschrieben.");
		ShapeFileWriter.writeGeometries(features, outputDir+"linksInArea.shp");
	}
	
	private static ArrayList<String> calcNodesInZone(Collection<SimpleFeature> zoneFeatures, 
			Collection<SimpleFeature> nodeFeatures){

		ArrayList<String> nodesInZone = new  ArrayList<String>();
		for (SimpleFeature zoneFeature : zoneFeatures){
			for(SimpleFeature networkFeature : nodeFeatures){ 
				Geometry zoneGeometry = (Geometry) zoneFeature.getDefaultGeometry();
				Geometry networkGeometry = (Geometry) networkFeature.getDefaultGeometry();
				if(zoneGeometry.contains(networkGeometry)) {
					nodesInZone.add(networkFeature.getAttribute("ID").toString());
				}
			}
		}
		return nodesInZone;
	}
	
	private static Network convertCoordinates(Network net, String fromCS, String toCS){

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(fromCS, toCS);

		for(Node node : net.getNodes().values()){
			Coord newCoord = ct.transform(node.getCoord());
			((Node)node).setCoord(newCoord);
		}

		return net;
	}
	
	private static void createDir(File file) {
		System.out.println("Verzeichnis " + file + " erstellt: "+ file.mkdirs());	
	}
}
