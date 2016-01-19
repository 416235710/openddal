/*
 * Copyright 2014-2016 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.openddal.server.mysql.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.openddal.server.util.ParseUtil;

/**
 * @author mycat
 */
public final class ServerParseShow {

	public static final int OTHER = -1;
	public static final int DATABASES = 1;
	public static final int DATASOURCES = 2;
	public static final int MYCAT_STATUS = 3;
	public static final int MYCAT_CLUSTER = 4;
	public static final int TABLES = 5;

	public static int parse(String stmt, int offset) {
		int i = offset;
		for (; i < stmt.length(); i++) {
			switch (stmt.charAt(i)) {
			case ' ':
				continue;
			case '/':
			case '#':
				i = ParseUtil.comment(stmt, i);
				continue;
			case 'M':
			case 'm':
				return mycatCheck(stmt, i);
			case 'D':
			case 'd':
				return dataCheck(stmt, i);
			case 'T':
			case 't':
				return tableCheck(stmt, i);
			default:
				return OTHER;
			}
		}
		return OTHER;
	}

	// SHOW MYCAT_
	static int mycatCheck(String stmt, int offset) {
		if (stmt.length() > offset + "ycat_?".length()) {
			char c1 = stmt.charAt(++offset);
			char c2 = stmt.charAt(++offset);
			char c3 = stmt.charAt(++offset);
			char c4 = stmt.charAt(++offset);
			char c5 = stmt.charAt(++offset);
			if ((c1 == 'Y' || c1 == 'y') && (c2 == 'C' || c2 == 'c')
					&& (c3 == 'A' || c3 == 'a') && (c4 == 'T' || c4 == 't')
					&& (c5 == '_')) {
				switch (stmt.charAt(++offset)) {
				case 'S':
				case 's':
					return showMyCatStatus(stmt, offset);
				case 'C':
				case 'c':
					return showMyCatCluster(stmt, offset);
				default:
					return OTHER;
				}
			}
		}
		return OTHER;
	}

	// SHOW MyCat_STATUS
	static int showMyCatStatus(String stmt, int offset) {
		if (stmt.length() > offset + "tatus".length()) {
			char c1 = stmt.charAt(++offset);
			char c2 = stmt.charAt(++offset);
			char c3 = stmt.charAt(++offset);
			char c4 = stmt.charAt(++offset);
			char c5 = stmt.charAt(++offset);
			if ((c1 == 't' || c1 == 'T')
					&& (c2 == 'a' || c2 == 'A')
					&& (c3 == 't' || c3 == 'T')
					&& (c4 == 'u' || c4 == 'U')
					&& (c5 == 's' || c5 == 'S')
					&& (stmt.length() == ++offset || ParseUtil.isEOF(stmt
							.charAt(offset)))) {
				return MYCAT_STATUS;
			}
		}
		return OTHER;
	}

	// SHOW MyCat_CLUSTER
	static int showMyCatCluster(String stmt, int offset) {
		if (stmt.length() > offset + "luster".length()) {
			char c1 = stmt.charAt(++offset);
			char c2 = stmt.charAt(++offset);
			char c3 = stmt.charAt(++offset);
			char c4 = stmt.charAt(++offset);
			char c5 = stmt.charAt(++offset);
			char c6 = stmt.charAt(++offset);
			if ((c1 == 'L' || c1 == 'l')
					&& (c2 == 'U' || c2 == 'u')
					&& (c3 == 'S' || c3 == 's')
					&& (c4 == 'T' || c4 == 't')
					&& (c5 == 'E' || c5 == 'e')
					&& (c6 == 'R' || c6 == 'r')
					&& (stmt.length() == ++offset || ParseUtil.isEOF(stmt
							.charAt(offset)))) {
				return MYCAT_CLUSTER;
			}
		}
		return OTHER;
	}

	// SHOW DATA
	static int dataCheck(String stmt, int offset) {
		if (stmt.length() > offset + "ata?".length()) {
			char c1 = stmt.charAt(++offset);
			char c2 = stmt.charAt(++offset);
			char c3 = stmt.charAt(++offset);
			if ((c1 == 'A' || c1 == 'a') && (c2 == 'T' || c2 == 't')
					&& (c3 == 'A' || c3 == 'a')) {
				switch (stmt.charAt(++offset)) {
				case 'B':
				case 'b':
					return showDatabases(stmt, offset);
				case 'S':
				case 's':
					return showDataSources(stmt, offset);
				default:
					return OTHER;
				}
			}
		}
		return OTHER;
	}

	// SHOW TABLE

	public static int tableCheck(String stmt, int offset) {

		// strict match
		String pat1 = "^\\s*(SHOW)\\s+(TABLES)\\s*";
		String pat2 = "^\\s*(SHOW)\\s+(TABLES)\\s+(LIKE\\s+'(.*)')\\s*";
		String pat3 = "^\\s*(SHOW)\\s+(TABLES)\\s+(FROM)\\s+([a-zA-Z_0-9]+)\\s*";
		String pat4 = "^\\s*(SHOW)\\s+(TABLES)\\s+(FROM)\\s+([a-zA-Z_0-9]+)\\s+(LIKE\\s+'(.*)')\\s*";

		boolean flag = isShowTableMatched(stmt, pat1);
		if (flag) {
			return TABLES;
		}

		flag = isShowTableMatched(stmt, pat2);
		if (flag) {
			return TABLES;
		}

		flag = isShowTableMatched(stmt, pat3);
		if (flag) {
			return TABLES;
		}

		flag = isShowTableMatched(stmt, pat4);
		if (flag) {
			return TABLES;
		}

		return OTHER;

	}

	private static boolean isShowTableMatched(String stmt, String pat1) {
		Pattern pattern = Pattern.compile(pat1, Pattern.CASE_INSENSITIVE);
		Matcher ma = pattern.matcher(stmt);

		return ma.matches();
	}

	// SHOW DATABASES
	static int showDatabases(String stmt, int offset) {
		if (stmt.length() > offset + "ases".length()) {
			char c1 = stmt.charAt(++offset);
			char c2 = stmt.charAt(++offset);
			char c3 = stmt.charAt(++offset);
			char c4 = stmt.charAt(++offset);
			if ((c1 == 'A' || c1 == 'a')
					&& (c2 == 'S' || c2 == 's')
					&& (c3 == 'E' || c3 == 'e')
					&& (c4 == 'S' || c4 == 's')
					&& (stmt.length() == ++offset || ParseUtil.isEOF(stmt
							.charAt(offset)))) {
				return DATABASES;
			}
		}
		return OTHER;
	}

	// SHOW DATASOURCES
	static int showDataSources(String stmt, int offset) {
		if (stmt.length() > offset + "ources".length()) {
			char c1 = stmt.charAt(++offset);
			char c2 = stmt.charAt(++offset);
			char c3 = stmt.charAt(++offset);
			char c4 = stmt.charAt(++offset);
			char c5 = stmt.charAt(++offset);
			char c6 = stmt.charAt(++offset);
			if ((c1 == 'O' || c1 == 'o')
					&& (c2 == 'U' || c2 == 'u')
					&& (c3 == 'R' || c3 == 'r')
					&& (c4 == 'C' || c4 == 'c')
					&& (c5 == 'E' || c5 == 'e')
					&& (c6 == 'S' || c6 == 's')
					&& (stmt.length() == ++offset || ParseUtil.isEOF(stmt
							.charAt(offset)))) {
				return DATASOURCES;
			}
		}
		return OTHER;
	}

}