/*
 * Copyright 2020 DaveLaw, https://github.com/DanskerDave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.krysalis.barcode4j.tools;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Helper class to apply a custom message pattern (i.e. message-character grouping) to a barcode.<br>
 * <br>
 * Based on an original, published by Jeremias Märki &amp; Dietmar Bürkle.
 * The original processed Strings byte-by-byte &amp; did not work correctly with Unicode.
 * (for example, the European Currency Symbol, otherwise known as "Euro", was handled incorrectly)
 * This version uses the same public API as the original,
 * but has been completely rewritten to work with Unicode Charsets.<br>
 * <br>
 * Apart from the Unicode support, there are 3 differences:<br>
 * - a Pattern Character ('!') was added to ignore the reminder of the message<br>
 * - '?' is substituted for missing (ESCAPE'd) Pattern-Characters<br>
 * - '?' is substituted for missing (PLACEHOLDER'ed) Message-Characters<br>
 * 
 * @author DaveLaw
 */
public class MessagePatternUtil {

	/**
	 * A simplistic wrapper for Unicode.<br>
	 * High-Low Surrogate-Pairs will be stored together.<br>
	 * All other Characters are stored as a single char.
	 */
	private static final class SimpleUnicode {

		private final char[] chars;

		public SimpleUnicode(final char... chars) {
			this.chars  = chars;
		}
		/**
		 * Parses a
		 * {@link String}
		 * to a
		 * {@link Queue}
		 * of
		 * {@link SimpleUnicode}.
		 * 
		 * @param string
		 * @return
		 */
		public static Queue<SimpleUnicode> parse(final String string) {

			final Queue<SimpleUnicode> charQueue = new LinkedList<>();

			final char[]               chars     = string.toCharArray();

			for (int hix = 0; hix < chars.length; hix++) {

				final int  lox  = hix + 1;
				final char high = chars[hix];

				if (lox >= chars.length
				||  Character.isHighSurrogate(high) == false) {
					charQueue.add(new SimpleUnicode(high));
					continue;
				}

				final char low  = chars[lox];

				if (Character.isLowSurrogate(low)) {
					System.out.println("Kanji.: " + high + low);
					hix++;
					charQueue.add(new SimpleUnicode(high, low));
				} else {
					charQueue.add(new SimpleUnicode(high));
				}
			}
			return charQueue;
		}
	}

	/**
	 * Escape-Character.: the next Unicode Character in the Pattern will be copied to the result.<br>
	 * It will neither be evaluated as an {@code ESCAPE} nor as an {@code ACTION}.<br>
	 * (if none is found {@link #QUESTION_MARK} will be inserted instead)
	 */
	public  static final char   ESCAPE                  = '\\';

	/**
	 * Placeholder.: the next Unicode Character in the Message will be copied to the result.<br>
	 * (if none is found {@link #QUESTION_MARK} will be inserted instead)
	 */
	public  static final char   ACTION_PLACEHOLDER      = '_';

	/**
	 * Delete.: the next Unicode Character in the Message will NOT be copied to the result.<br>
	 */
	public  static final char   ACTION_DELETE           = '#';

	/**
	 * Delete Remainder.: the rest of the Message will NOT be copied to the result.<br>
	 */
	public  static final char   ACTION_DELETE_REMAINDER = '!';

	/**
	 * Replacement if no Unicode Character is available in the Message or Pattern respectively.<br>
	 */
	public  static final char[] QUESTION_MARK           = {'?'};

	/**
	 * Format a Unicode Message using the supplied Unicode Pattern.<br>
	 * Characters in the Pattern are evaluated as follows:<br>
	 * - {@link #ACTION_DELETE_REMAINDER DELETE_REMAINDER} : all remaining <i>Message</i> characters are ignored<br>
	 * - {@link #ACTION_DELETE           DELETE}           : the next Unicode <i>Message</i> character is ignored<br>
	 * - {@link #ACTION_PLACEHOLDER      PLACEHOLDER}      : the next Unicode <i>Message</i> character is copied to the result<br>
	 * - {@link #ESCAPE}                                   : the next Unicode <i>Pattern</i> character is copied to the result<br>
	 * - <b><i>all others</i></b>                          : this Unicode <i>Pattern</i> character is copied to the result<br>
	 * <br>
	 * It is only necessary to use {@link #ESCAPE} if one of the above {@code ACTION} characters is to be inserted in the result.<br>
	 * <br>
	 * After evaluation of the Pattern, the remaining Message characters will be appended to the result.<br>
	 * If Message or Pattern are null or empty, Message will be returned as is.<br>
	 * <br>
	 * This Method should be able to handle Unicode (High-Low Surrogate-Pairs).<br>
	 * <br>
	 * If a character expected by the Pattern (either in the Message, or in the Pattern itself) is missing,
	 * a {@link #QUESTION_MARK} will be substituted.
	 * 
	 * @param   msg      the original Message
	 * @param   pattern  the Pattern to be applied to the Message
	 * @return           the formatted result
	 */
	public static String applyCustomMessagePattern(final String msg, final String pattern) {

		if (pattern == null || "".equals(pattern)
		||  msg     == null || "".equals(msg)) {
			return msg;
		}

		final StringBuilder        result       = new StringBuilder();

		final Queue<SimpleUnicode> msgQueue     = SimpleUnicode.parse(msg);
		final Queue<SimpleUnicode> patternQueue = SimpleUnicode.parse(pattern);

		while (patternQueue.isEmpty() == false) {

			final SimpleUnicode patternUnicode = patternQueue.remove();

			switch             (patternUnicode.chars[0]) {
				case ESCAPE                  :  poll(patternQueue, result);    break;
				case ACTION_PLACEHOLDER      :  poll(msgQueue,     result);    break;
				case ACTION_DELETE           :       msgQueue.poll();          break;
				case ACTION_DELETE_REMAINDER :       msgQueue.clear();         break;

				default                      :  result.append(patternUnicode.chars);
			}
		}
		/*
		 * Copy what's left of the Message to the result...
		 */
		msgQueue.forEach(msgUnicode -> result.append(msgUnicode.chars));
 
		return result.toString();
	}

	private static void poll(final Queue<SimpleUnicode> queue, final StringBuilder result) {

		final SimpleUnicode   nextUnicode  =  queue.poll();

		result.append(null != nextUnicode  ?  nextUnicode.chars  :  QUESTION_MARK);
	}
}
