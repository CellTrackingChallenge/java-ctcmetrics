/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2017, Vladimír Ulman
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.celltrackingchallenge.measures.util;

import java.util.Set;
import java.util.TreeSet;
import java.text.ParseException;


public class NumberSequenceHandler
{
	/** Attempts to parse input 'inStr' and returns 'true' only if that can be done. */
	public static
	boolean isValidInput(final String inStr)
	{
		boolean inputIsOK = true;

		//dry-run parse to see if it is parse-able
		try {
			parseSequenceOfNumbers(inStr,null);
		}
		catch (ParseException e)
		{
			inputIsOK = false;
		}

		return inputIsOK;
	}


	/** Attempts to parse input 'inStr' and returns 'null' only if that can be done,
	    otherwise a string with complaint message is returned. */
	public static
	String whyIsInputInvalid(final String inStr)
	{
		String complaintMsg = null;

		//dry-run parse to see if it is parse-able
		try {
			parseSequenceOfNumbers(inStr,null);
		}
		catch (ParseException e)
		{
			complaintMsg = e.getMessage();
		}

		return complaintMsg;
	}


	/** Parses the input 'inStr' and returns an expanded set that corresponds
	    to the (succint) string input. */
	public static
	TreeSet<Integer> toSet(final String inStr)
	{
		try {
			TreeSet<Integer> outList = new TreeSet<>();
			parseSequenceOfNumbers(inStr,outList);
			return outList;
		}
		catch (ParseException e)
		{
			throw new RuntimeException(e.getMessage());
		}
	}


	/** Parses the input 'inStr' and adds to the 'outList' an expanded set
	    that corresponds to the (succint) string input. Note the output
	    is not explicitly cleared in this method. */
	public static
	void toSet(final String inStr, final Set<Integer> outList)
	{
		try {
			parseSequenceOfNumbers(inStr,outList);
		}
		catch (ParseException e)
		{
			throw new RuntimeException(e.getMessage());
		}
	}
	// -------------------------------------------------------------------------

	/** Reads inStr and parses it into outList (if outList is not null).
	    This is the string-to-set conversion workhorse. */
	public static
	void parseSequenceOfNumbers(final String inStr, final Set<Integer> outList)
	throws ParseException
	{
		//marker where we pretend that the input string begins
		//aka "how much has been parsed so far"
		int strFakeBegin = 0;

		try {
			while (strFakeBegin < inStr.length())
			{
				int ic = inStr.indexOf(',',strFakeBegin);
				int ih = inStr.indexOf('-',strFakeBegin);
				//NB: returns -1 if not found

				if (ic == -1)
					//if no comma is found, then we are processing the last term
					ic = inStr.length();

				if (ih == -1)
					//if no hyphen is found, then go to the "comma" branch
					ih = ic+1;

				if (ic < ih)
				{
					//"comma branch"
					//we're parsing out N,
					int N = Integer.parseInt( inStr.substring(strFakeBegin, ic).trim() );
					if (outList != null)
						outList.add(N);
				}
				else
				{
					//"hyphen branch"
					//we're parsing out N-M,
					int N = Integer.parseInt( inStr.substring(strFakeBegin, ih).trim() );
					int M = Integer.parseInt( inStr.substring(ih+1, ic).trim() );
					if (outList != null)
						for (int n=N; n <= M; ++n)
							outList.add(n);
				}

				strFakeBegin = ic+1;
			}
		}
		catch (NumberFormatException e)
		{
			throw new ParseException("Parsing problem after reading "
			                         +inStr.substring(0,strFakeBegin)+": "+e.getMessage(),0);
		}
	}
}
