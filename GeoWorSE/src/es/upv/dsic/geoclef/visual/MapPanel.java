package es.upv.dsic.geoclef.visual;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Ellipse2D;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JPanel;

import es.upv.dsic.geoclef.GeoWorSE;
import es.upv.dsic.geoclef.geography.WorldPoint;
import es.upv.dsic.geoclef.search.GeoSearch;

public class MapPanel extends JPanel {
		Polygon p;
		Polygon chp;
		
	private int [] getCoordsFor(WorldPoint p){
		int [] coords = new int[2];
		int width = 400;
		int height = 380;
		int [] center = new int[2];
		center[0]=(int)((float)width/2);
		center[1]=(int)((float)height/2);
		//System.err.println("width: "+width+", height: "+height);
		//System.err.println("center: "+center[0]+", "+center[1]);
		
		coords[1]=-(int)(p.yCoord()*center[1])/90 + center[1];
		coords[0]=(int)(p.xCoord()*center[0])/180 + center[0];
		
		return coords;
	}
		
	  public MapPanel(List<WorldPoint> shapeArea) {
		super();		
		Iterator<WorldPoint> itr = shapeArea.iterator();
		p=new Polygon();
		while(itr.hasNext()){
			int [] coords = getCoordsFor(itr.next());
			p.addPoint(coords[0], coords[1]);
			//System.err.println("coords: "+coords[0]+", "+coords[1]);
		}
		
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(Color.black);
		List<WorldPoint> points = GeoWorSE.geoWNdict.getWorld();
		Iterator<WorldPoint> itr = points.iterator();
		while(itr.hasNext()){
			int [] coords = getCoordsFor(itr.next());
			g.drawRect(coords[0], coords[1], 1, 1);
		}
		
		if(p.npoints > 2){
			g.setColor(Color.blue);
			g.drawPolygon(this.p);
			if(chp!= null){
				g.setColor(Color.red);
				g.drawPolygon(this.chp);
			}
		} else {
			g.setColor(Color.green);
			for(int i=0; i< p.npoints; i++){
				g.fillOval(p.xpoints[i], p.ypoints[i], 6, 6);
			}
		}
		
	  }

	public void drawCH(List<WorldPoint> shapeArea) {
		Iterator<WorldPoint> itr = shapeArea.iterator();
		chp=new Polygon();
		while(itr.hasNext()){
			int [] coords = getCoordsFor(itr.next());
			chp.addPoint(coords[0], coords[1]);
			//System.err.println("coords: "+coords[0]+", "+coords[1]);
		}
		this.repaint();
		
	}
	}
