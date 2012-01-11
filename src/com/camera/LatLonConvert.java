package com.camera;

public class LatLonConvert
{
	// declare local variables used throughout the class
	private double dfDecimal;		// decimal degrees
	private double dfDegree;		// degree part of degrees/minutes/seconds
	private double dfMinute;		// minute part of degrees/minutes/seconds
	private double dfSecond;		// second part of degrees/minutes/seconds

	
/******************************************************************************
*	method:						LatLonConvert
*******************************************************************************
*				
*	The two constructors for LatLonConvert class accept either
*	
*	- a single double, which is interpreted as decimal degrees to be 
*		converted to degrees/minutes/seconds, or
*	
*	- three doubles, which are interpreted as values of degrees, minutes, 
*		and seconds, respectively, to be converted to decimal degrees.
*	
*	Member of LatLonConvert class
*				
* -------------------------------------------------------------------------- */

	// This constructor converts decimal degrees to degrees/minutes/seconds
	public LatLonConvert(
							double dfDecimalIn
						)
	{
		// load local variables
		dfDecimal = dfDecimalIn;

		// call appropriate conversion method
		fromDec2DMS();
	}

	// This constructor converts degrees/minutes/seconds to decimal degrees
	public LatLonConvert(
							double dfDegreeIn,
							double dfMinuteIn,
							double dfSecondIn
						)
	{
		// load local variables
		dfDegree = dfDegreeIn;
		dfMinute = dfMinuteIn;
		dfSecond = dfSecondIn;

		// call appropriate conversion method
		fromDMS2Dec();
	}

	
/******************************************************************************
*	method:					fromDec2DMS()
*******************************************************************************
*
*   Converts decimal degrees to degrees/minutes/seconds.
*				
*	Member of LatLonConvert class
*				
* -------------------------------------------------------------------------- */
	private void fromDec2DMS()
	{
		// define variables local to this method
		double dfFrac;			// fraction after decimal
		double dfSec;			// fraction converted to seconds

		// Get degrees by chopping off at the decimal
		dfDegree = Math.floor( dfDecimal );
		// correction required since floor() is not the same as int()
		if ( dfDegree < 0 )
			dfDegree = dfDegree + 1;

		// Get fraction after the decimal
		dfFrac = Math.abs( dfDecimal - dfDegree );

		// Convert this fraction to seconds (without minutes)
		dfSec = dfFrac * 3600;

		// Determine number of whole minutes in the fraction
		dfMinute = Math.floor( dfSec / 60 );

		// Put the remainder in seconds
		dfSecond = dfSec - dfMinute * 60;

		// Fix rounoff errors
		if ( Math.rint( dfSecond ) == 60 )
		{
			dfMinute = dfMinute + 1;
			dfSecond = 0;
		}

		if ( Math.rint( dfMinute ) == 60 )
		{
			if ( dfDegree < 0 )
				dfDegree = dfDegree - 1;
			else // ( dfDegree => 0 )
				dfDegree = dfDegree + 1;

			dfMinute = 0;
		}

		return;
	}

	
/******************************************************************************
*	method:					fromDMS2Dec()
*******************************************************************************
*
*   Converts degrees/minutes/seconds to decimal degrees.
*				
*	Member of LatLonConvert class
*				
* -------------------------------------------------------------------------- */
	private void fromDMS2Dec()
	{
		// define variables local to this method
		double dfFrac;					// fraction after decimal

		// Determine fraction from minutes and seconds
		dfFrac = dfMinute / 60 + dfSecond / 3600;

		// Be careful to get the sign right. dfDegIn is the only signed input.
		if ( dfDegree < 0 )
			dfDecimal = dfDegree - dfFrac;
		else
			dfDecimal = dfDegree + dfFrac;

		return;
	}

	
/******************************************************************************
*	method:					getDecimal()
*******************************************************************************
*
*   Gets the value in decimal degrees.
*				
*	Member of LatLonConvert class
*				
* -------------------------------------------------------------------------- */
	public double getDecimal()
	{
		return( dfDecimal );
	}

	
/******************************************************************************
*	method:					getDegree()
*******************************************************************************
*
*   Gets the degree part of degrees/minutes/seconds.
*				
*	Member of LatLonConvert class
*				
* -------------------------------------------------------------------------- */
	public double getDegree()
	{
		return( dfDegree );
	}

	
/******************************************************************************
*	method:					getMinute()
*******************************************************************************
*
*   Gets the minute part of degrees/minutes/seconds.
*				
*	Member of LatLonConvert class
*				
* -------------------------------------------------------------------------- */
	public double getMinute()
	{
		return( dfMinute );
	}

	
/******************************************************************************
*	method:					getSecond()
*******************************************************************************
*
*   Gets the second part of degrees/minutes/seconds.
*				
*	Member of LatLonConvert class
*				
* -------------------------------------------------------------------------- */
	public double getSecond()
	{
		return( dfSecond );
	}

	
}
