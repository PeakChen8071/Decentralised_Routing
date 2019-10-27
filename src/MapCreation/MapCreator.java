package MapCreation;

import COMSETsystem.*;
import DataParsing.GeoProjector;
import DataParsing.KdTree;

import java.io.*;
import java.util.*;

import UserExamples.Cluster;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author Robert van Barlingen
 * mail Bobby.van.Barlingen@gmail.com
 * @author Bo xu
 * mail bo.5.xu@here.com
 *
 *  Reads the JSON file generated by OpenStreetMaps and creates a directed map based on this file.
 *  Two types of elements are read from the JSON file, namely "nodes" and "ways". A node can represent 
 *  many things, from a traffic sign, to a road bump, to a point of a road curve, and to an intersection. 
 *  A way is a straight line connecting two nodes. In COMSET, a node is called a "vertex" and a way is 
 *  called a "link". (See class Vertex and Link for more details).
 *
 *  Once vertices and links are read from the JSON file, the map creator removes all the dead end vertices,
 *  namely the vertices that have no incoming links or no outgoing links.
 *  
 *  The map creator then identifies the vertices that connect different streets and promote these vertices
 *  to "intersections". "Roads" are formed to connect intersections. Each road may consists of one or more 
 *  links from the JSON file. For example, if the road between two intersection is a curve, then there may 
 *  be multiple links (and multiple vertices) to provide a polyline approximation of the curve.  
 *  
 *  The MapCreator also provides the ability to group vertices. This implies that all the
 *  vertices which lie within a certain distance of each other are converted to a single vertex.
 *  This reduces the number of intersections roughly by a factor of 2.
 */
public class MapCreator {

	// Default speed limits in km/h associated with each OpenStreetMaps road type when maxspeed is not explicitly specified,
	// according to https://github.com/fossgis-routing-server/osrm-backend/blob/master/profiles/car.lua
	final static int speedMotorway = 90;
	final static int speedMotorwayLink = 45;
	final static int speedTrunk = 85;
	final static int speedTrunkLink = 40;
	final static int speedPrimary = 65;
	final static int speedPrimaryLink = 30;
	final static int speedSecondary = 55;
	final static int speedSecondaryLink = 25;
	final static int speedTertiary = 40;
	final static int speedTertiaryLink = 20;
	final static int speedUnclassified = 25;
	final static int speedResidential = 25;
	final static int speedLivingStreet = 10;
	final static int speedDefault = 15;

	final static double kilometersPerMile = 1.60934; // 1 mile = 1.60934 kilometer

	// Distance in meters used to group vertices together if they're too close
	final double minimumDistance = 25;

	// Map from id's to intersections. This represents the map.
	Map<Long, Intersection> intersections;

	// Map from id's to vertices.
	Map<Long, Vertex> vertices;

	// Used to set id's of newly added vertices, such that they're always unique.
	long idCounter;

	// Used to project from lat,lon to x,y in meters
	GeoProjector projector;

	// The bounding polygon for cropping the map.
	// Created from boundingPolygonKMLFile
	static List<double[]> boundingPolygon;

	/**
	 * Constructor of the MapCreator class. Reads the JSON file defined by fileName and
	 * converts it into a map represented by { @code vertices ).
	 * Uses Json.simple package
	 *
	 * @param fileName the JSON file that will be read
	 * @param boundingPolygonKMLFile a KML file defining a bounding polygon of the simulated area
	 * modifies {@code vertices }
	 *
	 */
	public MapCreator(String mapFile, String boundingPolygonKMLFile, double speedReduction) {

		boundingPolygon = getPolygonFromKML(boundingPolygonKMLFile);

		// Initialize intersections to be a TreeMap
		intersections = new TreeMap<>();

		// Initialize vertices to be a TreeMap.
		vertices = new TreeMap<>(); 

		JSONParser parser = new JSONParser();
		try {
			// read file
			Reader reader = new FileReader(mapFile);
			// create JSONObject based on the file
			Object obj = parser.parse(reader);
			JSONObject jsonObject = (JSONObject) obj;

			// loop over all the elements in the JSON file to set all the vertices
			JSONArray elements = (JSONArray) jsonObject.get("elements");
			boolean firstVertex = true;
			for (Object elementObject : elements) {
				JSONObject element = (JSONObject) elementObject;
				String type = (String) element.get("type");
				// if the type is a vertex, then create an Vertex
				if (type.equals("node")) {
					long id = (long) element.get("id");
					double latitude = (double) element.get("lat");
					double longitude = (double) element.get("lon");
					if (firstVertex) {
						this.projector = new GeoProjector(latitude, longitude);
						firstVertex = false;
					}
					double xy[] = projector.fromLatLon(latitude, longitude);
					vertices.put(id, new Vertex(longitude, latitude, xy[0], xy[1], id));
				}
			}

			// loop over all the elements again to set the roads
			for (Object elementObject : elements) {
				JSONObject element = (JSONObject) elementObject;
				String type = (String) element.get("type");
				if (type.equals("way")) { // check if it's a road
					String highway;
					double maxSpeed; // speed limit in km/h
					boolean oneway = false;
					Object tagsObject = element.get("tags");
					JSONObject tags = (JSONObject) tagsObject;
					highway = (String)tags.get("highway");
					// set speed limit
					if (tags.containsKey("maxspeed")) {
						String speedString = (String) tags.get("maxspeed");
						if( speedString.contains(" mph")) {
							speedString = speedString.replace(" mph", "");
							maxSpeed = Integer.parseInt(speedString) * kilometersPerMile; // convert from mph to kmph
						} else {
							// default unit is km/h
							maxSpeed = Integer.parseInt(speedString);
						}
					} else {
						switch(highway) {
						case "motorway":
							maxSpeed = speedMotorway;
							break;
						case "motorway_link":
							maxSpeed = speedMotorwayLink;
							break;
						case "trunk":
							maxSpeed = speedTrunk;
							break;
						case "trunk_link":
							maxSpeed = speedTrunkLink;
							break;
						case "primary":
							maxSpeed = speedPrimary;
							break;
						case "primary_link":
							maxSpeed = speedPrimaryLink;
							break;
						case "secondary":
							maxSpeed = speedSecondary;
							break;
						case "secondary_link":
							maxSpeed = speedSecondaryLink;
							break;
						case "tertiary":
							maxSpeed = speedTertiary;
							break;
						case "tertiary_link":
							maxSpeed = speedTertiaryLink;
							break;
						case "unclassified":
							maxSpeed = speedUnclassified;
							break;
						case "residential":
							maxSpeed = speedResidential;
							break;
						case "living_street":
							maxSpeed = speedLivingStreet;
							break;
						default:
							maxSpeed = speedDefault;
							break;
						}
					}
					// check if it's a one way street
					if(tags.containsKey("oneway") && ((String)tags.get("oneway")).equals("yes")) {
						oneway = true;
					}
					// set roads
					JSONArray jsonvertices = (JSONArray) element.get("nodes");
					int length = jsonvertices.size();
					for (int i = 0; i < length - 1; i++) {
						long id1 = (long)jsonvertices.get(i);
						long id2 = (long)jsonvertices.get(i+1);

						double distance = vertices.get(id1).xy.distance(vertices.get(id2).xy);

						// Convert km/h to meters per second; apply speed reduction
						vertices.get(id1).addEdge(vertices.get(id2), distance, maxSpeed * 1000 / 3600 / speedReduction);
						if (!oneway) {
							vertices.get(id2).addEdge(vertices.get(id1), distance, maxSpeed * 1000 / 3600 / speedReduction);
						}
					}
				}
			}

		// handle exceptions
		} catch (FileNotFoundException e) {
			System.out.println("error FileNotFoundException");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("error IOException");
			e.printStackTrace();
		} catch (ParseException e) {
			System.out.println("error ParseException");
			e.printStackTrace();
		}
		
		setIdCounter();
	}

	/**
	 * Create a map.
	 * @return map
	 */
	public CityMap createMap() {
		// Crop the map using the bounding polygon
		cropMap();

		// Consolidate the map by removing dead-end vertices and aggregating close-by vertices.
		clearAndGroup();

		// Create intersections from vertices that connect different streets.
		createIntersections();
		
		// Create roads from links to connect intersections.
		createRoads();

		// Output the map
		CityMap cityMap = outputCityMap();
		
		return cityMap;
	}


	/**
	 * Crop a map using the bounding polygon by removing vertices that are outside the polygon.
	 *
	 * modifies {@code vertices }
	 *
	 */
	public void cropMap() {
		Object[] idObjects = vertices.keySet().toArray();
		boundingPolygon.add(boundingPolygon.get(0));
		for (Object idObj : idObjects) {
			long id = (long) idObj;
			Vertex vertex = vertices.get(id);
			if (!insidePolygon(vertex.longitude, vertex.latitude)) {
				vertex.severVertex();
				vertices.remove(id);
			}
		}
	}

	/**
	 * Check if a location (x,y) is inside the bounding polygon.
	 * @param x x coordinate of the location to check against the polygon
	 * @param y y coordinate of the location to check against the polygon
	 * @return
	 */
	public static boolean insidePolygon(double x, double y) {
		int count = 0;
		for (int i = 0; i < boundingPolygon.size() - 1; i++) {
			double x1 = boundingPolygon.get(i)[0];
			double y1 = boundingPolygon.get(i)[1];
			double x2 = boundingPolygon.get(i+1)[0];
			double y2 = boundingPolygon.get(i+1)[1];
			double beta = (y - y1)/(y2 - y1);
			double alpha = x1 + beta * (x2-x1) - x;
			if (alpha > 0 && 0 <= beta && beta <= 1) {
				count++;
			}
		}
		count = count % 2;
		if (count == 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Construct the bounding polygon from a KML file
	 * @param polygonKMLFile a KML file defining a polygon
	 * @return a polygon represented by a list of [x,y] coordinates
	 */
	public static List<double[]> getPolygonFromKML(String polygonKMLFile) {
		String regex = "^\\s+";
		String line = "";
		List<double[]> polygon = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(polygonKMLFile))) {
			// A quick and dirty KML parser to get polygon coordinates from a KML file that contains a single polygon.
			while ((line = br.readLine()) != null) {

				String trimmedLine = line.replaceAll(regex,  "");
				if (!trimmedLine.equals("<coordinates>"))
					continue;
				while (!(line = br.readLine()).replaceAll(regex, "").equals("</coordinates>")) {
					trimmedLine = line.replaceAll(regex,  "");
					String[] vertices = line.split(" ");
					for (int i = 0; i < vertices.length; i++) {
						String vertex = vertices[i];
						String[] lonLat = vertex.split(",");
						double[] pair = {Double.valueOf(lonLat[0]), Double.valueOf(lonLat[1])};
						polygon.add(pair);
					}
				}
				break;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return polygon;
	}
	
	/**
	 *  Sets the idCounter to the maximum id in vertices and adds 1
	 *
	 * @modifes { @code idCounter }
	 */
	private void setIdCounter() {
		idCounter = -1L;
		for (long id : vertices.keySet()) {
			if (idCounter <= id) {
				idCounter = id;
			}
		}
		idCounter++;
	}


	/**
	 * One iteration of the ClearMap function. Goes over every vertex
	 * and determines if it's a dead end.
	 * If that's the case then this vertex is removed along with the links
	 * attached to it. 
	 * In order to fully clear the whole map multiple iterations of this algorithm
	 * are needed because it is possible that after removing certain vertices
	 * in the first iteration, previous dead-end streets are now defined by other
	 * vertices. These vertices then have to be removed in the second
	 * iteration.
	 *
	 * modifies {@code vertices }
	 */
	public void clearMapIteration() throws IllegalStateException{
		// set of vertices (ids) that need to be removed
		Set<Long> toRemove = new TreeSet<>();
		// go over every vertex
		for (long id : vertices.keySet()) {
			Vertex vertex = vertices.get(id);
			Set<Link> roadsFrom = vertex.getLinksFrom();
			Set<Link> roadsTo = vertex.getLinksTo();
			// Remove vertices that have no outgoing roads or incoming roads. 
			if (roadsFrom.size() == 0 || roadsTo.size() == 0) { 
				toRemove.add(id);
				continue;
			}
		}
		// remove all the vertices that have to be removed
		for (Long id : toRemove) {
			vertices.get(id).cutVertex();
			vertices.remove(id);
		}
	}

	/**
	 * Identify vertices that connect different streets and promote them to intersections
	 */
	public void createIntersections() {

		// go over every vertex
		for (long id : vertices.keySet()) {
			Vertex vertex = vertices.get(id);
			Set<Link> roadsFrom = vertex.getLinksFrom();
			Set<Link> roadsTo = vertex.getLinksTo();
			// Skip vertices that connect two one way links. 
			if ((roadsFrom.size() == 1 && roadsTo.size() == 1) && !(roadsFrom.toArray(new Link[1])[0].to == roadsTo.toArray(new Link[1])[0].from)) {
				// do not promote
				continue;
			}


			// if there are two incoming and two outgoing links for a vertex
			// then it's possible that that vertex lies in the middle of
			// a two way street. Therefore this has to be checked, and if this is
			// the case it has to be skipped.
			if (roadsFrom.size() == 2 && roadsTo.size() == 2) {
				boolean checkForAll = true;
				for (Vertex interTo: vertex.getAdjacentTo()) {
					boolean check = false;
					for (Vertex interFrom : vertex.getAdjacentFrom()) {
						if (interTo.equals(interFrom)) {
							check = true;
						}
					}
					if (!check) {
						checkForAll = false;
					}
				}
				if (checkForAll) {
					// do not promote
					continue;
				}
			}
			// promote to intersection
			promoteIntersection(vertex);
		}
	}
	
	/**
	 * Promote a vertex to an intersection.
	 * @param vertex the vertex to promote
	 */
	public void promoteIntersection(Vertex vertex) {
		Intersection intersection= new Intersection(vertex);
		intersections.put(intersection.id, intersection);
		vertex.intersection = intersection;
	}

	/**
	 * Create roads to connect intersections
	 */
	public void createRoads() {
		for (Intersection intersection : intersections.values()) {
			Vertex vertex = intersection.vertex;
			for (Link link : vertex.linksMapFrom.values()) {
				// create a new road
				Road road = new Road();
				road.from = intersection;

				// extend the road by visiting non-intersection vertices one by one until
				// an intersection is reached
				Link currentLink = link;
				while (currentLink.to.intersection == null) {
					road.addLink(currentLink);
					for (Link linkFrom : currentLink.to.linksMapFrom.values()) {
						if (linkFrom.to != currentLink.from) {
							currentLink = linkFrom;
							break;
						}
					}
				}
				// add the link that connects to the end intersection
				road.addLink(currentLink);
				road.to = currentLink.to.intersection;
				intersection.roadsMapFrom.put(road.to, road);
				road.to.roadsMapTo.put(intersection,  road);
			}
		}
	}

	/**
	 * Removes all dead end vertices, i.e. the vertices that do not have 
	 * incoming links or outgoing links.
	 *
	 * modifies {@code vertices }
	 */
	public void clearMap() {
		int previousNumberVertices = vertices.size();
		// number of vertices.size() can never be negative, therefore it
		// always runs at least once.
		int newNumberVertices = -2;
		while (previousNumberVertices != newNumberVertices) {
			clearMapIteration();
			previousNumberVertices = newNumberVertices;
			newNumberVertices = vertices.size();
		}
	}

	/**
	 * A single iteration in the process of grouping all the vertices
	 * together.
	 * In a single iteration every pair of connected vertices is checked
	 * and if the distance between the vertices is smaller than the input
	 * distance, then the two vertices are grouped together. This means
	 * that a single new vertex is added with incoming and outgoing links
	 * identical to the combination of the two original vertices.
	 *
	 * @param distance the minimum distance such that two vertices are grouped together
	 * modifies {@code vertices }
	 */
	public void groupVerticesIteration(double distance) {
		for (long id : vertices.keySet()) {
			Vertex interFrom = vertices.get(id);
			// check if the distance between two connected vertices is smaller than
			// the minimum distance
			for (Link link : interFrom.getLinksFrom()) {
				if (link.from.distanceTo(link.to) <= distance) {
					Vertex interTo = link.to;
					double newLongtitude = (interFrom.longitude + interTo.longitude)/2;
					double newLatitude = (interFrom.latitude + interTo.latitude)/2;
					double newXY[] = projector.fromLatLon(newLatitude, newLongtitude);
					// it is very important that the id of newInter is unique!
					Vertex newInter = new Vertex(newLongtitude, newLatitude, newXY[0], newXY[1], idCounter++);  
					for (Link inter1From : interFrom.getLinksFrom()) {
						if (inter1From.to.id != interTo.id) {
							newInter.addEdge(inter1From.to, newInter.distanceTo(inter1From.to), inter1From.speed);
						}
					}
					for (Link inter1To : interFrom.getLinksTo()) {
						inter1To.from.addEdge(newInter, newInter.distanceTo(inter1To.from), inter1To.speed);
					}
					for (Link inter2From : interTo.getLinksFrom()) { 
						newInter.addEdge(inter2From.to, newInter.distanceTo(inter2From.to), inter2From.speed);
					}
					for (Link inter2To : interTo.getLinksTo()) {
						if (inter2To.from.id != interFrom.id) {
							inter2To.from.addEdge(newInter, newInter.distanceTo(inter2To.from), inter2To.speed);
						}
					}                    interFrom.severVertex();
					interTo.severVertex();
					vertices.remove(interFrom.id);
					vertices.remove(interTo.id);
					vertices.put(newInter.id, newInter);
					return;
				}
			}
		}
	}

	/**
	 * Groups vertices together which lie close together. "close together"
	 * is defined by the minimumDisatnce variable.
	 * Grouping together entails that vertices whose distance from each other
	 * is less than minimumDistance, are replaced by a single vertex whose
	 * incoming and outgoing vertices are the same as the combination of the grouped
	 * vertices.
	 * This is accomplished by running groupVerticesIteration until no additional
	 * vertices are grouped together
	 *
	 * modifies {@code vertices }
	 */
	public void groupVertices() {
		int previousNumberVertices = vertices.size();
		// number of vertices.size() can never be negative, therefore it
		// always runs at least once.
		int newNumberVertices = -2;
		while (previousNumberVertices != newNumberVertices) {
			groupVerticesIteration(minimumDistance);
			previousNumberVertices = newNumberVertices;
			newNumberVertices = vertices.size();
		}
	}

	public void fixMap() {
		boolean check = true;
		while (check) {
			check = false;
			Object[] ids = vertices.keySet().toArray();
			for (Object idObj : ids) {
				long id = (long) idObj;
				Vertex vertex = vertices.get(id);
				if (vertex.getAdjacentFrom().isEmpty()) {
					vertex.cutVertex();
					vertices.remove(id);
					check = true;
				}
			}
			Set<Long> reached = new TreeSet<>();
			long firstId = vertices.keySet().iterator().next();
			Vertex firstVertex = vertices.get(firstId);
			checkNeighbors(firstVertex, reached);
			if (reached.size() != vertices.keySet().size()) {
				ids = vertices.keySet().toArray();
				for (Object idObj : ids) {
					long id = (long) idObj;
					if (!reached.contains(id)) {
						Vertex vertex = vertices.get(id);
						vertex.cutVertex();
						vertices.remove(id);
						check = true;
					}
				}
			}
		}
	}

	public void checkNeighbors(Vertex inter, Set<Long> reached) {
		for (Vertex neighbor : inter.getAdjacentFrom()) {
			if (!reached.contains(neighbor.id)) {
				reached.add(neighbor.id);
				checkNeighbors(neighbor, reached);
			}
		}
	}

	/**
	 * Combines grouping and clearing of vertices until the map can not
	 * be simplified any further.
	 * Accomplished by clearing and grouping until no additional vertices
	 * are removed
	 */
	public void clearAndGroup() {
		int previousNumberVertices = vertices.size();
		// number of vertices.size() can never be negative, therefore it
		// always runs at least once.
		int newNumberVertices = -2;
		while (previousNumberVertices != newNumberVertices) {
			clearMap();
			// It seems unnecessary to group vertices given the size of the map we are considering.
			// groupVertices();
			previousNumberVertices = newNumberVertices;
			newNumberVertices = vertices.size();
		}
		fixMap();
	}


	/**
	 * Returns an instance of CityMap representing the map it created.
	 * 
	 * @return an instance of CityMap
	 */
	public CityMap outputCityMap() {
		KdTree kdTree = new KdTree();
		for (Vertex vertex : vertices.values()) {
			for (Link link : vertex.getLinksFrom()) {
				kdTree.insert(link);	
			}			
		}
		List<Road> roads = new ArrayList<>();
		for (Intersection inter : intersections.values()) {
			for (Road road : inter.getRoadsFrom()) {
				roads.add(road);
			}
		}
		//modification
		HashMap<Long, Double> PR = new HashMap<>();
		Scanner sc = null;
		try {
			sc = new Scanner(new File("ClusterData/pagerank.csv"));
			sc.useDelimiter(",|\n");    //scanner will skip over "," and "\n" found in file
			sc.nextLine(); // skip the header
			while (sc.hasNext()) {
				String str = sc.nextLine();
				//road_id,page_rank
				String[] s = str.split(",");
				Long roadId = Long.parseLong(s[0]);
				Double rank = Double.parseDouble(s[1]);
				PR.put(roadId, rank);

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}


		for (Road r:roads){
			r.rating  = r.travelTime;

			if (PR.containsKey(r.id)){
				//System.out.println(PR.get(r.id)/r.length*5000000 + " "+r.rating);
				r.rating = (long) Math.min(r.rating, r.length/PR.get(r.id));
			}
		}
		return new CityMap(intersections, roads, projector, kdTree);
	}

	public List<double[]> boundingPolygon() {
		return boundingPolygon;
	}

	/**
	 * @return { @code projector }
	 */
	public GeoProjector projector() {
		return projector;
	}    

}
