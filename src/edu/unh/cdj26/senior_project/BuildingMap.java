
package edu.unh.cdj26.senior_project;

import android.content.Context;
import android.graphics.drawable.*;
import android.view.*;
import android.graphics.*;
import java.util.*;


public class BuildingMap extends View
               implements GestureDetector.OnGestureListener,
                          ScaleGestureDetector.OnScaleGestureListener
                        
{

   private Drawable mapImage;
   private int mapWidth, mapHeight;
   private float xLoc, yLoc;
   private float xScreen, yScreen;
   private Location locRelative;
   private GestureDetector gestures;
   private ScaleGestureDetector scaleDetector;
   private ArrayList<ActiveAPState> actives;
   private boolean newWifiData;
   private float scale;
   private PointF userLocation;

   private float foc_x_old, foc_y_old;

      // access points will be passed in
   public BuildingMap( Context c )
   {
      super( c );
      mapImage = c.getResources().getDrawable( 
                  R.drawable.floorplan );

      Bitmap bitmap = BitmapFactory.decodeResource(
         getResources(), R.drawable.floorplan );
      mapWidth = bitmap.getWidth();
      mapHeight = bitmap.getHeight();

      setUpperLeftPixel( 0, 0 );

      gestures = new GestureDetector( c, this ); 
      scaleDetector = new ScaleGestureDetector( c, this );


      actives = new ArrayList<ActiveAPState>();

      newWifiData = false;
      scale = 0.5f;
   }

   @Override
   protected void onDraw( Canvas canvas )
   {
      super.onDraw( canvas );
      canvas.save();

      float x, y;
      switch( locRelative )
      {
         case UpperLeft:
            mapImage.setBounds( (int) (-xLoc * scale),
                                (int) (-yLoc * scale), 
                                (int) ((-xLoc + mapWidth)  * scale), 
                                (int) ((-yLoc + mapHeight) * scale) );
            break;
         case Center:
            x = xLoc - getWidth()/(2*scale);
            y = yLoc - getHeight()/(2*scale);
            mapImage.setBounds( (int) (-x * scale), 
                                (int) (-y * scale),
                                (int) ((-x + mapWidth)  * scale),
                                (int) ((-y + mapHeight) * scale) );
            break;
         case Pixel:
            x = xLoc - xScreen/scale;
            y = yLoc - yScreen/scale;
            mapImage.setBounds( (int) (-x * scale), 
                                (int) (-y * scale),
                                (int) ((-x + mapWidth)  * scale),
                                (int) ((-y + mapHeight) * scale) );
            break;
      }

      mapImage.draw( canvas );
     
     
      Paint brush = new Paint();
      brush.setDither( true );
      brush.setAntiAlias( true );
      brush.setStrokeJoin( Paint.Join.ROUND );
      brush.setStrokeCap( Paint.Cap.ROUND );
      brush.setStrokeWidth( 2 );

      Rect bounds = mapImage.getBounds();
      
      List<AccessPoint> aps = IndoorLocalization.getAPs();
      if( newWifiData )
      {
         for( AccessPoint ap : aps )
            ap.saveState();

         newWifiData = false;
      }

      for( AccessPoint ap : aps )
      {
         float apX = ap.getX(); 
         float apY = ap.getY();

         apX *= scale;
         apY *= scale;
         
         apX += bounds.left;
         apY += bounds.top;

         Path p = new Path();
         p.moveTo( apX - 5, apY + 4 );
         p.rLineTo( 10, 0 );
         p.rLineTo( -5, -10 );
         p.rLineTo( -5, 10 );

         brush.setColor( 0xFFDD7700 );
         brush.setStyle( Paint.Style.STROKE );
         canvas.drawPath( p, brush );
      
         brush.setColor( 0x55DD7700 );
         brush.setStyle( Paint.Style.FILL );
         canvas.drawPath( p, brush );
        
         AccessPoint state = ap.getSavedState(); 
         boolean debugTemp = true;
         if( state != null && state.hasNewLevel() )
         {
            /*
            int rss = state.peekLevel();

            double d_m = Math.pow( 10, ( rss + 47 ) / (-27.5) );

            double h = state.getHeight();

            double rad_m = Math.sqrt( d_m*d_m - h*h );
            double rad_p = rad_m * 33.1 * scale;

            if( debugTemp )
            {

               System.err.println( " --- " );
               System.err.println( "rss:      " + rss );
               System.err.println( "distance: " + d_m );
               System.err.println( "radius:   " + rad_m );

               debugTemp = false;

            }

            brush.setColor( 0x44CC1111 );
            brush.setStyle( Paint.Style.STROKE );
            canvas.drawCircle( (float) apX,
                               (float) apY, 
                               (float) rad_p,
                               brush );
            */
         }
      }

      if( userLocation != null )
      {
         brush.setColor( 0xFF2222CC );
         brush.setStyle( Paint.Style.FILL );
         canvas.drawCircle( userLocation.x * scale + bounds.left, 
                            userLocation.y * scale + bounds.top,
                            4, brush );
      }


      canvas.restore();
   }

   @Override
   public boolean onTouchEvent( MotionEvent me )
   {
      scaleDetector.onTouchEvent( me );

      if( scaleDetector.isInProgress() )
         return true;

      gestures.onTouchEvent(me);
      switch( me.getAction() )
      {
         /* No longer need action up moves to center of screen
         case MotionEvent.ACTION_UP:
            Rect bounds = mapImage.getBounds();

            float x = (me.getX() - bounds.left) / scale;
            float y = (me.getY() - bounds.top) / scale;
            System.err.println( x + ", " +  y );
            setCenterPixel( (int)x, (int)y );
            break;
          */
      }

      return true;
      //return gestures.onTouchEvent( me );
   }

   public void setCenterPixel( float x, float y )
   {
      locRelative = Location.Center;
      xLoc = x;
      yLoc = y;
      invalidate();
   }

   public void setUpperLeftPixel( float x, float y )
   {
      locRelative = Location.UpperLeft;
      xLoc = x;
      yLoc = y;
      invalidate();
   }
   
   public void setPixelRelativeTo( float sX, float sY, float pX, float pY )
   {
      locRelative = Location.Pixel;
      xScreen = sX;
      yScreen = sY;

      xLoc = pX;
      yLoc = pY;

      invalidate();
   }

   private enum Location
   {
      UpperLeft, Center, Pixel
   }

   public void newWifiData( float x, float y )
   {
      newWifiData = true;
      userLocation = new PointF( x, y );
      invalidate();
   }

   @Override
   public boolean onScroll( MotionEvent me1, MotionEvent me2, float dX, float dY )
   {
      xLoc += dX/scale;
      yLoc += dY/scale;

      invalidate();

      return true;
   }

   @Override
   public boolean onDown( MotionEvent me )
   {
      return true;
   }

   @Override
   public boolean onFling( MotionEvent me1, MotionEvent me2, float vX, float vY )
   {
      return false;
   }

   @Override
   public void onLongPress( MotionEvent me )
   {
   }

   @Override
   public void onShowPress( MotionEvent me )
   {
   }

   @Override
   public boolean onSingleTapUp( MotionEvent me )
   {
      return false;
   }

   @Override
   public boolean onScale( ScaleGestureDetector sgd )
   {
      float foc_x = sgd.getFocusX();
      float foc_y = sgd.getFocusY();

      /* 
      float foc_x_ave = ( foc_x_old + foc_x ) / 2;
      float foc_y_ave = ( foc_y_old + foc_y ) / 2;

      foc_x_old = foc_x;
      foc_y_old = foc_y;
      float bx = mapImage.getBounds().left;
      float by = mapImage.getBounds().top;

      float fy = foc_y_old;

      float newX = (fx-bx)/scale;
      float newY = (fy-by)/scale;
      */


      scale *= sgd.getScaleFactor();

      scale = Math.max( 0.25f, Math.min( scale, 1.0f ) );

      setPixelRelativeTo( foc_x, foc_y, foc_x_old, foc_y_old );

      return true;
   }

   @Override
   public boolean onScaleBegin( ScaleGestureDetector sgd )
   {
      foc_x_old = sgd.getFocusX();
      foc_y_old = sgd.getFocusY();

      foc_x_old -= mapImage.getBounds().left;
      foc_y_old -= mapImage.getBounds().top;

      foc_x_old /= scale;
      foc_y_old /= scale;

      return true;
   }

   @Override
   public void onScaleEnd( ScaleGestureDetector sgd )
   {
   }

   public static float metersToPixels( float meters )
   {
      return meters * 33.1f;
   }

   private class ActiveAPState
   {
      public float x_px, y_px, inner, outer;

      public ActiveAPState( float x, float y,
                            float i, float o )
      {
         x_px = x;
         y_px = y;
         inner = i;
         outer = o;
      }
   }
}
