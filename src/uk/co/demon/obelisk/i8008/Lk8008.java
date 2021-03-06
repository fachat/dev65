/*
 * Copyright (C),2017 Andrew John Jacobs.
 *
 * This program is provided free of charge for educational purposes
 *
 * Redistribution and use in binary form without modification, is permitted
 * provided that the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS 'AS IS' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.co.demon.obelisk.i8008;

import uk.co.demon.obelisk.xlnk.Linker;

/**
 * The <CODE>Lk8008</CODE> provides access to the base <CODE>Linker</CODE> code
 * for the Intel 8008 suite.
 *
 * @author 	Andrew Jacobs
 * @version	$Id$
 */
public class Lk8008 extends Linker
{
	/**
	 * Main program entry point.
	 * 
	 * @param arguments		Command line arguments.
	 */
	public static void main (String arguments [])
	{
		new Lk8008 ().run (arguments);
	}

	/**
	 * Constructs a <CODE>Lk4004</CODE> instance.
	 */
	protected Lk8008 ()
	{
		super (8);
	}
		
	/**
	 * {@inheritDoc}
	 */
	protected int getAddrSize ()
	{
		return (16);
	}
}