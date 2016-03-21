package earthQuakeMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.utils.MapUtils;
import parsing.ParseFeed;
import processing.core.PApplet;

/** EarthquakeCityMap
 * An application with an interactive map displaying earthquake data.
 * @author Ivo Rakar
 * Author: UC San Diego Intermediate Software Development MOOC team
 * Date: March 2, 2016
 * */
public class EarthquakeCityMap extends PApplet {
	
	
	
	private static final long serialVersionUID = 1L;

	

	//feed with magnitude 2.5+ Earthquakes
	private String earthquakesURL = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.atom";
	//variable used to control if the initialisation fase in over
	private boolean initialize;
	// The files containing city names and info and country names and info
	private String cityFile = "city-data.json";
	private String countryFile = "countries.geo.json";
	
	// The map
	private UnfoldingMap map;
	
	// Markers for each city
	private List<Marker> cityMarkers;
	// Markers for each earthquake
	private List<Marker> quakeMarkers;
	// A List of country markers
	private List<Marker> countryMarkers;
	//A list of countries with at least one earthquake
	private List<Marker> countryQuakeMarkers;
	//A sorted array containing the Earthquakes
	private Object[] sortedQuakeMarkersArray;
	
	//Variables to maintain the state of the clicked Markers
	private CommonMarker lastSelected;
	private CommonMarker lastClicked;
	private Marker lastCountrySelected;
	
	
	public void setup() {		
		// (1) Initializing canvas and map tiles
		size(1024, 700, OPENGL);
		initialize = true;
		
		map = new UnfoldingMap(this, 200, 50, 650, 600, new Google.GoogleMapProvider());
		MapUtils.createDefaultEventDispatcher(this, map);
		
		//Reading in earthquake data and geometric properties
	    //load country features and markers
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);
		setCountriesTransparent();
		//read in city data
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for(Feature city : cities) {
		  cityMarkers.add(new CityMarker(city));
		}
	    countryQuakeMarkers = new ArrayList<Marker>();
		//read in earthquake RSS feed
	    List<PointFeature> earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
	    quakeMarkers = new ArrayList<Marker>();
	    
	    for(PointFeature feature : earthquakes) {
		  //check if LandQuake
		  if(isLand(feature)) {
		    quakeMarkers.add(new LandQuakeMarker(feature));
		  }
		  // OceanQuakes
		  else {
		    quakeMarkers.add(new OceanQuakeMarker(feature));
		  }
	    }
	 		
	    //  Markers added to map
	    map.addMarkers(countryMarkers);
	    map.addMarkers(quakeMarkers);
	    map.addMarkers(cityMarkers);
	    
	    //Sort the earthquakes by their magnitude
	    sortedQuakeMarkersArray = quakeMarkers.toArray();
		Arrays.sort(sortedQuakeMarkersArray);
	    
	}  // End setup
	
	/**Method that is automatically executed in loop and draws in the PApplet */
	public void draw() {
		background(140,140,140);
		map.draw();
		addKey();
		addEarthquakeList();
		addCountryList();
		
	}
	
	//Helper method to unColor the countries
	private void setCountriesTransparent() 
	{
		for(Marker marker : countryMarkers) {
			marker.setColor(color(255,255,255,0));
		}
	}
	
	//Helper method to sort and print the earthquakes
	private void printEarthquakeList() 
	{
		int maxNum=20;
		if(maxNum>sortedQuakeMarkersArray.length) {
			maxNum=sortedQuakeMarkersArray.length;
		}
		int xbase = 875;
		int ybase = 400;
		
		for(int i=0;i<maxNum;i++) {
			
			fill(0);
			textSize(12);
			text(((EarthquakeMarker)sortedQuakeMarkersArray[i]).getTitle(), xbase+5, ybase+i*12);
			
		}
	}
	
	/** Event handler that gets called automatically when the 
	 * mouse moves.
	 */
	@Override
	public void mouseMoved() 
	{
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;
		
		}
		if(lastCountrySelected != null) {
			lastCountrySelected.setSelected(false);
			lastCountrySelected.setColor(color(255,255,255,0));
			lastCountrySelected = null;
		}
		
		selectMarkerIfHover(quakeMarkers);
		selectMarkerIfHover(cityMarkers);
		if(mouseX>200 && mouseX<850 && mouseY>50 && mouseY<650) {
			selectMarkerIfHoverCountry();
		}
		
		//loop();
	}
	
	//Colors in red the country underneath the mouse pointer
	private void selectMarkerIfHoverCountry() 
	{
		if (lastCountrySelected != null) {
			return;
		}
		for(Marker country : countryMarkers) {	
			// looping over markers making up MultiMarker
			if(country.getClass() == MultiMarker.class) {
				for(Marker marker : ((MultiMarker)country).getMarkers()) {
					// checking if inside
					if(((AbstractShapeMarker)marker).isInside(map,  mouseX, mouseY)) {
						lastCountrySelected = country;
						country.setColor(color(250,105,55,90));	
						return;
					}
				}
			}
			// check if inside country represented by SimplePolygonMarker
			else if(((AbstractShapeMarker)country).isInside(map,  mouseX, mouseY)) {
				lastCountrySelected = country;
				country.setColor(color(250,105,55,90));
				return;
			}
		}
	}
		
		
	
	
	// If there is a marker selected 
	private void selectMarkerIfHover(List<Marker> markers) 
	{
		// Abort if there's already a marker selected
		if (lastSelected != null) {
			return;
		}
		
		for (Marker m : markers) 
		{
			CommonMarker marker = (CommonMarker)m;
			if (marker.isInside(map,  mouseX, mouseY)) {
				lastSelected = marker;
				marker.setSelected(true);
				return;
			}
		}
	}
	
	/** The event handler for mouse clicks
	 * It will display an earthquake and its threat circle of cities
	 * Or if a city is clicked, it will display all the earthquakes 
	 * where the city is in the threat circle
	 */
	@Override
	public void mouseClicked()
	{
		if (lastClicked != null) {
			unhideMarkers();
			lastClicked = null;
		}
		else if (lastClicked == null) 
		{
			checkEarthquakesForClick();
			if (lastClicked == null) {
				checkCitiesForClick();
				checkEarthquakeListForClick();
			}
		}
	}
	
	/** The event handler for mouse release
	 * Calculates the position of the mouse release to determinate 
	 * whether to select, a country, an earthquake or nothing
	 */
	@Override
	public void mouseReleased()
	{
		
		if(mouseX>850 && mouseX<1020 && mouseY>85 && mouseY<300) {
			int posY = mouseY-85;
			int indexState = (posY/12)-1;
			setCountriesTransparent();
			countryQuakeMarkers.get(indexState).setColor(color(50,50,255,120));
		}
	}
	
	// Helper method that will check if a city marker was clicked on
	// and respond appropriately
	private void checkCitiesForClick()
	{
		if (lastClicked != null) return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker marker : cityMarkers) {
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker)marker;
				// Hide all the other earthquakes and hide
				for (Marker mhide : cityMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : quakeMarkers) {
					EarthquakeMarker quakeMarker = (EarthquakeMarker)mhide;
					if (quakeMarker.getDistanceTo(marker.getLocation()) 
							> quakeMarker.threatCircle()) {
						quakeMarker.setHidden(true);
					}
				}
				return;
			}
		}		
	}
	
	// Helper method that will check if an earthquake marker was clicked on
	// and respond appropriately
	private void checkEarthquakesForClick()
	{
		if (lastClicked != null) return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker m : quakeMarkers) {
			EarthquakeMarker marker = (EarthquakeMarker)m;
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = marker;
				// Hide all the other earthquakes and hide
				for (Marker mhide : quakeMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : cityMarkers) {
					if (mhide.getDistanceTo(marker.getLocation()) 
							> marker.threatCircle()) {
						mhide.setHidden(true);
					}
				}
				return;
			}
		}
	}
	
	//Helper method to control if the user clicked to the list of earthquakes
	private void checkEarthquakeListForClick()
	{
		if(mouseX>875 && mouseX<1205 && mouseY>393 && mouseY<640) {
			int posY = mouseY-385;
			int indexQuake = (posY/12)-1;
			lastClicked=(EarthquakeMarker)sortedQuakeMarkersArray[indexQuake];
			for (Marker mhide : quakeMarkers) {
				if (mhide != lastClicked) {
					mhide.setHidden(true);
				}
			}
			for (Marker mhide : cityMarkers) {
				mhide.setHidden(true);
			}
		}
	}
		
	// loop over and unhide all markers
	private void unhideMarkers() {
		for(Marker marker : quakeMarkers) {
			marker.setHidden(false);
		}
			
		for(Marker marker : cityMarkers) {
			marker.setHidden(false);
		}	
	}
	
	// helper method to draw key in GUI
	private void addKey() {	
		// Remember you can use Processing's graphics methods here
		fill(255, 250, 240);
		
		int xbase = 25;
		int ybase = 50;
		
		rect(xbase, ybase, 150, 250);
		
		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Earthquake Key", xbase+25, ybase+25);
		
		fill(150, 30, 30);
		int tri_xbase = xbase + 35;
		int tri_ybase = ybase + 50;
		triangle(tri_xbase, tri_ybase-CityMarker.TRI_SIZE, tri_xbase-CityMarker.TRI_SIZE, 
				tri_ybase+CityMarker.TRI_SIZE, tri_xbase+CityMarker.TRI_SIZE, 
				tri_ybase+CityMarker.TRI_SIZE);

		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
		text("City Marker", tri_xbase + 15, tri_ybase);
		
		text("Land Quake", xbase+50, ybase+70);
		text("Ocean Quake", xbase+50, ybase+90);
		text("Size ~ Magnitude", xbase+25, ybase+110);
		
		fill(255, 255, 255);
		ellipse(xbase+35, 
				ybase+70, 
				10, 
				10);
		rect(xbase+35-5, ybase+90-5, 10, 10);
		
		fill(color(255, 255, 0));
		ellipse(xbase+35, ybase+140, 12, 12);
		fill(color(0, 0, 255));
		ellipse(xbase+35, ybase+160, 12, 12);
		fill(color(255, 0, 0));
		ellipse(xbase+35, ybase+180, 12, 12);
		
		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Shallow", xbase+50, ybase+140);
		text("Intermediate", xbase+50, ybase+160);
		text("Deep", xbase+50, ybase+180);

		text("Past hour", xbase+50, ybase+200);
		
		fill(255, 255, 255);
		int centerx = xbase+35;
		int centery = ybase+200;
		ellipse(centerx, centery, 12, 12);

		strokeWeight(2);
		line(centerx-8, centery-8, centerx+8, centery+8);
		line(centerx-8, centery+8, centerx+8, centery-8);
		
		
	}
	
	//Helper method to add the list of earthquakes to the GUI
	private void addEarthquakeList()
	{
		fill(255, 250, 240);
		
		int xbase = 875;
		int ybase = 350;
		
		rect(xbase, ybase, 330, 300);
		
		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Earthquake list", xbase+100, ybase+25);
		
		printEarthquakeList();
		//Shades the row under the mouse pointer in the list of earthquakes
		if(mouseX>875 && mouseX<1205 && mouseY>393 && mouseY<640) {
			int posY = mouseY-385;
			int indexQuake = (posY/12)-1;
			fill(255,200,210,50);
			rect(xbase, (indexQuake*12)+395, 330, 12);
		}
		
	}

	//Draws the right rectangle for the listing of the states
	private void addCountryList(){
		
		fill(255, 250, 240);
		
		int xbase = 875;
		int ybase = 50;
		
		rect(xbase, ybase, 330, 270);
		
		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Country list", xbase+10, ybase+25);
		text("Earthquake number",xbase+145, ybase+25);
		
		//method used to print the states and the number of earthquakes
		printCountryQuakes(xbase, ybase+35);
		//Shades the row under the mouse pointer in the list of countries
		if(mouseX>875 && mouseX<1205 && mouseY>93 && mouseY<300) {
			int posY = mouseY-85;
			int indexState = (posY/12)-1;
			fill(255,200,210,50);
			rect(xbase, (indexState*12)+93, 330, 12);
		}
		
		
	}
	
	// Checks whether this quake occurred on land.  If it did, it sets the 
	// "country" property of its PointFeature to the country where it occurred
	// and returns true.  Notice that the helper method isInCountry will
	// set this "country" property already.  Otherwise it returns false.
	private boolean isLand(PointFeature earthquake) {
		
		// Loop over all countries to check if location is in any of them
		// If it is, add 1 to the entry in countryQuakes corresponding to this country.
		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}
		
		// not inside any country
		return false;
	}
	
	//Helper method to print the list of countries on a canvas
	private void printCountryQuakes(int xbase, int ybase) {
		int numRow = 0;
		for (Marker country : countryMarkers) {
			
			String countryName = country.getStringProperty("name");
			int numQuakes = 0;
			for (Marker marker : quakeMarkers) {
				EarthquakeMarker eqMarker = (EarthquakeMarker)marker;
				if (eqMarker.isOnLand()) {
					if (countryName.equals(eqMarker.getStringProperty("country"))) {
						numQuakes++;
						if(initialize && numQuakes==1)
						{
							countryQuakeMarkers.add(country);
						}
						
					}
				}
			}
			if (numQuakes > 0) {
				numRow++;
				fill(0);
				textSize(12);
				text(countryName, xbase+5, ybase+numRow*12);
				text(numQuakes, xbase+210, ybase+numRow*12);
			}
		}
		initialize = false;
	}
	
	
	
	// helper method to test whether a given earthquake is in a given country
	// This will also add the country property to the properties of the earthquake feature if 
	// it's in one of the countries.
	private boolean isInCountry(PointFeature earthquake, Marker country) {
		// getting location of feature
		Location checkLoc = earthquake.getLocation();

		// some countries represented it as MultiMarker
		// looping over SimplePolygonMarkers which make them up to use isInsideByLoc
		if(country.getClass() == MultiMarker.class) {
				
			// looping over markers making up MultiMarker
			for(Marker marker : ((MultiMarker)country).getMarkers()) {
					
				// checking if inside
				if(((AbstractShapeMarker)marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));
						
					// return if is inside one
					return true;
				}
			}
		}
			
		// check if inside country represented by SimplePolygonMarker
		else if(((AbstractShapeMarker)country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));
			
			return true;
		}
		return false;
	}

}
