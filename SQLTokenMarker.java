/*
 * SQLTokenMarker.java - Generic SQL token marker
 * Copyright (C) 1999 mike dillon
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

import javax.swing.text.Segment;

/**
 * SQL token marker.
 *
 * @author mike dillon
 * @version $Id: SQLTokenMarker.java,v 1.6 1999/04/19 05:38:20 sp Exp $
 */
public class SQLTokenMarker extends TokenMarker
{
	private int offset, lastOffset, lastKeyword, length;
    	private static KeywordMap sqlKeywords;

	// public members
        public SQLTokenMarker(){
	    this(getKeywords());
        }

	public SQLTokenMarker(KeywordMap k)
	{
		this(k, false);
	}


	public SQLTokenMarker(KeywordMap k, boolean tsql)
	{
		keywords = k;
		isTSQL = tsql;
	}


	public static KeywordMap getKeywords()
	{
    	    KeywordMap sqlKeywords = new KeywordMap(false);
	    sqlKeywords.add("ADD", Token.KEYWORD2);
	    sqlKeywords.add("ALL", Token.KEYWORD2);
	    sqlKeywords.add("ALTER", Token.KEYWORD2);
	    sqlKeywords.add("ANALYZE", Token.KEYWORD2);
	    sqlKeywords.add("AND", Token.KEYWORD2);
	    sqlKeywords.add("AS", Token.KEYWORD2);
	    sqlKeywords.add("ASC", Token.KEYWORD2);
	    sqlKeywords.add("ASENSITIVE", Token.KEYWORD2);
	    sqlKeywords.add("BEFORE", Token.KEYWORD2);
	    sqlKeywords.add("BETWEEN", Token.KEYWORD2);
	    sqlKeywords.add("BIGINT", Token.KEYWORD2);
	    sqlKeywords.add("BINARY", Token.KEYWORD2);
	    sqlKeywords.add("BLOB", Token.KEYWORD2);
	    sqlKeywords.add("BOTH", Token.KEYWORD2);
	    sqlKeywords.add("BY", Token.KEYWORD2);
	    sqlKeywords.add("CALL", Token.KEYWORD2);
	    sqlKeywords.add("CASCADE", Token.KEYWORD2);
	    sqlKeywords.add("CASE", Token.KEYWORD2);
	    sqlKeywords.add("CHANGE", Token.KEYWORD2);
	    sqlKeywords.add("CHAR", Token.KEYWORD2);
	    sqlKeywords.add("CHARACTER", Token.KEYWORD2);
	    sqlKeywords.add("CHECK", Token.KEYWORD2);
	    sqlKeywords.add("COLLATE", Token.KEYWORD2);
	    sqlKeywords.add("COLUMN", Token.KEYWORD2);
	    sqlKeywords.add("CONDITION", Token.KEYWORD2);
	    sqlKeywords.add("CONSTRAINT", Token.KEYWORD2);
	    sqlKeywords.add("CONTINUE", Token.KEYWORD2);
	    sqlKeywords.add("CONVERT", Token.KEYWORD2);
	    sqlKeywords.add("CREATE", Token.KEYWORD2);
	    sqlKeywords.add("CROSS", Token.KEYWORD2);
	    sqlKeywords.add("CURRENT_DATE", Token.KEYWORD2);
	    sqlKeywords.add("CURRENT_TIME", Token.KEYWORD2);
	    sqlKeywords.add("CURRENT_TIMESTAMP", Token.KEYWORD2);
	    sqlKeywords.add("CURRENT_USER", Token.KEYWORD2);
	    sqlKeywords.add("CURSOR", Token.KEYWORD2);
	    sqlKeywords.add("DATABASE", Token.KEYWORD2);
	    sqlKeywords.add("DATABASES", Token.KEYWORD2);
	    sqlKeywords.add("DAY_HOUR", Token.KEYWORD2);
	    sqlKeywords.add("DAY_MICROSECOND", Token.KEYWORD2);
	    sqlKeywords.add("DAY_MINUTE", Token.KEYWORD2);
	    sqlKeywords.add("DAY_SECOND", Token.KEYWORD2);
	    sqlKeywords.add("DEC", Token.KEYWORD2);
	    sqlKeywords.add("DECIMAL", Token.KEYWORD2);
	    sqlKeywords.add("DECLARE", Token.KEYWORD2);
	    sqlKeywords.add("DEFAULT", Token.KEYWORD2);
	    sqlKeywords.add("DELAYED", Token.KEYWORD2);
	    sqlKeywords.add("DELETE", Token.KEYWORD2);
	    sqlKeywords.add("DESC", Token.KEYWORD2);
	    sqlKeywords.add("DESCRIBE", Token.KEYWORD2);
	    sqlKeywords.add("DETERMINISTIC", Token.KEYWORD2);
	    sqlKeywords.add("DISTINCT", Token.KEYWORD2);
	    sqlKeywords.add("DISTINCTROW", Token.KEYWORD2);
	    sqlKeywords.add("DIV", Token.KEYWORD2);
	    sqlKeywords.add("DOUBLE", Token.KEYWORD2);
	    sqlKeywords.add("DROP", Token.KEYWORD2);
	    sqlKeywords.add("DUAL", Token.KEYWORD2);
	    sqlKeywords.add("EACH", Token.KEYWORD2);
	    sqlKeywords.add("ELSE", Token.KEYWORD2);
	    sqlKeywords.add("ELSEIF", Token.KEYWORD2);
	    sqlKeywords.add("ENCLOSED", Token.KEYWORD2);
	    sqlKeywords.add("ESCAPED", Token.KEYWORD2);
	    sqlKeywords.add("EXISTS", Token.KEYWORD2);
	    sqlKeywords.add("EXIT", Token.KEYWORD2);
	    sqlKeywords.add("EXPLAIN", Token.KEYWORD2);
	    sqlKeywords.add("FALSE", Token.KEYWORD2);
	    sqlKeywords.add("FETCH", Token.KEYWORD2);
	    sqlKeywords.add("FLOAT", Token.KEYWORD2);
	    sqlKeywords.add("FLOAT4", Token.KEYWORD2);
	    sqlKeywords.add("FLOAT8", Token.KEYWORD2);
	    sqlKeywords.add("FOR", Token.KEYWORD2);
	    sqlKeywords.add("FORCE", Token.KEYWORD2);
	    sqlKeywords.add("FOREIGN", Token.KEYWORD2);
	    sqlKeywords.add("FROM", Token.KEYWORD2);
	    sqlKeywords.add("FULLTEXT", Token.KEYWORD2);
	    sqlKeywords.add("GRANT", Token.KEYWORD2);
	    sqlKeywords.add("GROUP", Token.KEYWORD2);
	    sqlKeywords.add("HAVING", Token.KEYWORD2);
	    sqlKeywords.add("HIGH_PRIORITY", Token.KEYWORD2);
	    sqlKeywords.add("HOUR_MICROSECOND", Token.KEYWORD2);
	    sqlKeywords.add("HOUR_MINUTE", Token.KEYWORD2);
	    sqlKeywords.add("HOUR_SECOND", Token.KEYWORD2);
	    sqlKeywords.add("IF", Token.KEYWORD2);
	    sqlKeywords.add("IGNORE", Token.KEYWORD2);
	    sqlKeywords.add("IN", Token.KEYWORD2);
	    sqlKeywords.add("INDEX", Token.KEYWORD2);
	    sqlKeywords.add("INFILE", Token.KEYWORD2);
	    sqlKeywords.add("INNER", Token.KEYWORD2);
	    sqlKeywords.add("INOUT", Token.KEYWORD2);
	    sqlKeywords.add("INSENSITIVE", Token.KEYWORD2);
	    sqlKeywords.add("INSERT", Token.KEYWORD2);
	    sqlKeywords.add("INT", Token.KEYWORD2);
	    sqlKeywords.add("INT1", Token.KEYWORD2);
	    sqlKeywords.add("INT2", Token.KEYWORD2);
	    sqlKeywords.add("INT3", Token.KEYWORD2);
	    sqlKeywords.add("INT4", Token.KEYWORD2);
	    sqlKeywords.add("INT8", Token.KEYWORD2);
	    sqlKeywords.add("INTEGER", Token.KEYWORD2);
	    sqlKeywords.add("INTERVAL", Token.KEYWORD2);
	    sqlKeywords.add("INTO", Token.KEYWORD2);
	    sqlKeywords.add("IS", Token.KEYWORD2);
	    sqlKeywords.add("ITERATE", Token.KEYWORD2);
	    sqlKeywords.add("JOIN", Token.KEYWORD2);
	    sqlKeywords.add("KEY", Token.KEYWORD2);
	    sqlKeywords.add("KEYS", Token.KEYWORD2);
	    sqlKeywords.add("KILL", Token.KEYWORD2);
	    sqlKeywords.add("LEADING", Token.KEYWORD2);
	    sqlKeywords.add("LEAVE", Token.KEYWORD2);
	    sqlKeywords.add("LEFT", Token.KEYWORD2);
	    sqlKeywords.add("LIKE", Token.KEYWORD2);
	    sqlKeywords.add("LIMIT", Token.KEYWORD2);
	    sqlKeywords.add("LINES", Token.KEYWORD2);
	    sqlKeywords.add("LOAD", Token.KEYWORD2);
	    sqlKeywords.add("LOCALTIME", Token.KEYWORD2);
	    sqlKeywords.add("LOCALTIMESTAMP", Token.KEYWORD2);
	    sqlKeywords.add("LOCK", Token.KEYWORD2);
	    sqlKeywords.add("LONG", Token.KEYWORD2);
	    sqlKeywords.add("LONGBLOB", Token.KEYWORD2);
	    sqlKeywords.add("LONGTEXT", Token.KEYWORD2);
	    sqlKeywords.add("LOOP", Token.KEYWORD2);
	    sqlKeywords.add("LOW_PRIORITY", Token.KEYWORD2);
	    sqlKeywords.add("MATCH", Token.KEYWORD2);
	    sqlKeywords.add("MEDIUMBLOB", Token.KEYWORD2);
	    sqlKeywords.add("MEDIUMINT", Token.KEYWORD2);
	    sqlKeywords.add("MEDIUMTEXT", Token.KEYWORD2);
	    sqlKeywords.add("MIDDLEINT", Token.KEYWORD2);
	    sqlKeywords.add("MINUTE_MICROSECOND", Token.KEYWORD2);
	    sqlKeywords.add("MINUTE_SECOND", Token.KEYWORD2);
	    sqlKeywords.add("MOD", Token.KEYWORD2);
	    sqlKeywords.add("MODIFIES", Token.KEYWORD2);
	    sqlKeywords.add("NATURAL", Token.KEYWORD2);
	    sqlKeywords.add("NOT", Token.KEYWORD2);
	    sqlKeywords.add("NO_WRITE_TO_BINLOG", Token.KEYWORD2);
	    sqlKeywords.add("NULL", Token.KEYWORD2);
	    sqlKeywords.add("NUMERIC", Token.KEYWORD2);
	    sqlKeywords.add("ON", Token.KEYWORD2);
	    sqlKeywords.add("OPTIMIZE", Token.KEYWORD2);
	    sqlKeywords.add("OPTION", Token.KEYWORD2);
	    sqlKeywords.add("OPTIONALLY", Token.KEYWORD2);
	    sqlKeywords.add("OR", Token.KEYWORD2);
	    sqlKeywords.add("ORDER", Token.KEYWORD2);
	    sqlKeywords.add("OUT", Token.KEYWORD2);
	    sqlKeywords.add("OUTER", Token.KEYWORD2);
	    sqlKeywords.add("OUTFILE", Token.KEYWORD2);
	    sqlKeywords.add("PRECISION", Token.KEYWORD2);
	    sqlKeywords.add("PRIMARY", Token.KEYWORD2);
	    sqlKeywords.add("PROCEDURE", Token.KEYWORD2);
	    sqlKeywords.add("PURGE", Token.KEYWORD2);
	    sqlKeywords.add("READ", Token.KEYWORD2);
	    sqlKeywords.add("READS", Token.KEYWORD2);
	    sqlKeywords.add("REAL", Token.KEYWORD2);
	    sqlKeywords.add("REFERENCES", Token.KEYWORD2);
	    sqlKeywords.add("REGEXP", Token.KEYWORD2);
	    sqlKeywords.add("RELEASE", Token.KEYWORD2);
	    sqlKeywords.add("RENAME", Token.KEYWORD2);
	    sqlKeywords.add("REPEAT", Token.KEYWORD2);
	    sqlKeywords.add("REPLACE", Token.KEYWORD2);
	    sqlKeywords.add("REQUIRE", Token.KEYWORD2);
	    sqlKeywords.add("RESTRICT", Token.KEYWORD2);
	    sqlKeywords.add("RETURN", Token.KEYWORD2);
	    sqlKeywords.add("REVOKE", Token.KEYWORD2);
	    sqlKeywords.add("RIGHT", Token.KEYWORD2);
	    sqlKeywords.add("RLIKE", Token.KEYWORD2);
	    sqlKeywords.add("SCHEMA", Token.KEYWORD2);
	    sqlKeywords.add("SCHEMAS", Token.KEYWORD2);
	    sqlKeywords.add("SECOND_MICROSECOND", Token.KEYWORD2);
	    sqlKeywords.add("SELECT", Token.KEYWORD2);
	    sqlKeywords.add("SENSITIVE", Token.KEYWORD2);
	    sqlKeywords.add("SEPARATOR", Token.KEYWORD2);
	    sqlKeywords.add("SET", Token.KEYWORD2);
	    sqlKeywords.add("SHOW", Token.KEYWORD2);
	    sqlKeywords.add("SMALLINT", Token.KEYWORD2);
	    sqlKeywords.add("SONAME", Token.KEYWORD2);
	    sqlKeywords.add("SPATIAL", Token.KEYWORD2);
	    sqlKeywords.add("SPECIFIC", Token.KEYWORD2);
	    sqlKeywords.add("SQL", Token.KEYWORD2);
	    sqlKeywords.add("SQLEXCEPTION", Token.KEYWORD2);
	    sqlKeywords.add("SQLSTATE", Token.KEYWORD2);
	    sqlKeywords.add("SQLWARNING", Token.KEYWORD2);
	    sqlKeywords.add("SQL_BIG_RESULT", Token.KEYWORD2);
	    sqlKeywords.add("SQL_CALC_FOUND_ROWS", Token.KEYWORD2);
	    sqlKeywords.add("SQL_SMALL_RESULT", Token.KEYWORD2);
	    sqlKeywords.add("SSL", Token.KEYWORD2);
	    sqlKeywords.add("STARTING", Token.KEYWORD2);
	    sqlKeywords.add("STRAIGHT_JOIN", Token.KEYWORD2);
	    sqlKeywords.add("TABLE", Token.KEYWORD2);
	    sqlKeywords.add("TERMINATED", Token.KEYWORD2);
	    sqlKeywords.add("THEN", Token.KEYWORD2);
	    sqlKeywords.add("TINYBLOB", Token.KEYWORD2);
	    sqlKeywords.add("TINYINT", Token.KEYWORD2);
	    sqlKeywords.add("TINYTEXT", Token.KEYWORD2);
	    sqlKeywords.add("TO", Token.KEYWORD2);
	    sqlKeywords.add("TRAILING", Token.KEYWORD2);
	    sqlKeywords.add("TRIGGER", Token.KEYWORD2);
	    sqlKeywords.add("TRUE", Token.KEYWORD2);
	    sqlKeywords.add("UNDO", Token.KEYWORD2);
	    sqlKeywords.add("UNION", Token.KEYWORD2);
	    sqlKeywords.add("UNIQUE", Token.KEYWORD2);
	    sqlKeywords.add("UNLOCK", Token.KEYWORD2);
	    sqlKeywords.add("UNSIGNED", Token.KEYWORD2);
	    sqlKeywords.add("UPDATE", Token.KEYWORD2);
	    sqlKeywords.add("USAGE", Token.KEYWORD2);
	    sqlKeywords.add("USE", Token.KEYWORD2);
	    sqlKeywords.add("USING", Token.KEYWORD2);
	    sqlKeywords.add("UTC_DATE", Token.KEYWORD2);
	    sqlKeywords.add("UTC_TIME", Token.KEYWORD2);
	    sqlKeywords.add("UTC_TIMESTAMP", Token.KEYWORD2);
	    sqlKeywords.add("VALUES", Token.KEYWORD2);
	    sqlKeywords.add("VARBINARY", Token.KEYWORD2);
	    sqlKeywords.add("VARCHAR", Token.KEYWORD2);
	    sqlKeywords.add("VARCHARACTER", Token.KEYWORD2);
	    sqlKeywords.add("VARYING", Token.KEYWORD2);
	    sqlKeywords.add("WHEN", Token.KEYWORD2);
	    sqlKeywords.add("WHERE", Token.KEYWORD2);
	    sqlKeywords.add("WHILE", Token.KEYWORD2);
	    sqlKeywords.add("WITH", Token.KEYWORD2);
	    sqlKeywords.add("WRITE", Token.KEYWORD2);
	    sqlKeywords.add("XOR", Token.KEYWORD2);
	    sqlKeywords.add("YEAR_MONTH", Token.KEYWORD2);
	    sqlKeywords.add("ZEROFILL", Token.KEYWORD2);
	    return sqlKeywords;
	}



	public byte markTokensImpl(byte token, Segment line, int lineIndex)
	{
		offset = lastOffset = lastKeyword = line.offset;
		length = line.count + offset;

loop:
		for(int i = offset; i < length; i++)
		{
			switch(line.array[i])
			{
			case '*':
				if(token == Token.COMMENT1 && length - i >= 1 && line.array[i+1] == '/')
				{
					token = Token.NULL;
					i++;
					addToken((i + 1) - lastOffset,Token.COMMENT1);
					lastOffset = i + 1;
				}
				else if (token == Token.NULL)
				{
					searchBack(line, i);
					addToken(1,Token.OPERATOR);
					lastOffset = i + 1;
				}
				break;
			case '[':
				if(token == Token.NULL)
				{
					searchBack(line, i);
					token = Token.LITERAL1;
					literalChar = '[';
					lastOffset = i;
				}
				break;
			case ']':
				if(token == Token.LITERAL1 && literalChar == '[')
				{
					token = Token.NULL;
					literalChar = 0;
					addToken((i + 1) - lastOffset,Token.LITERAL1);
					lastOffset = i + 1;
				}
				break;
			case '.': case ',': case '(': case ')':
				if (token == Token.NULL) {
					searchBack(line, i);
					addToken(1, Token.NULL);
					lastOffset = i + 1;
				}
				break;
			case '+': case '%': case '&': case '|': case '^':
			case '~': case '<': case '>': case '=':
				if (token == Token.NULL) {
					searchBack(line, i);
					addToken(1,Token.OPERATOR);
					lastOffset = i + 1;
				}
				break;
			case ' ': case '\t':
				if (token == Token.NULL) {
					searchBack(line, i, false);
				}
				break;
			case ':':
				if(token == Token.NULL)
				{
					addToken((i+1) - lastOffset,Token.LABEL);
					lastOffset = i + 1;
				}
				break;
			case '/':
				if(token == Token.NULL)
				{
					if (length - i >= 2 && line.array[i + 1] == '*')
					{
						searchBack(line, i);
						token = Token.COMMENT1;
						lastOffset = i;
						i++;
					}
					else
					{
						searchBack(line, i);
						addToken(1,Token.OPERATOR);
						lastOffset = i + 1;
					}
				}
				break;
			case '-':
				if(token == Token.NULL)
				{
					if (length - i >= 2 && line.array[i+1] == '-')
					{
						searchBack(line, i);
						addToken(length - i,Token.COMMENT1);
						lastOffset = length;
						break loop;
					}
					else
					{
						searchBack(line, i);
						addToken(1,Token.OPERATOR);
						lastOffset = i + 1;
					}
				}
				break;
			case '!':
				if(isTSQL && token == Token.NULL && length - i >= 2 &&
				(line.array[i+1] == '=' || line.array[i+1] == '<' || line.array[i+1] == '>'))
				{
					searchBack(line, i);
					addToken(1,Token.OPERATOR);
					lastOffset = i + 1;
				}
				break;
			case '"': case '\'':
				if(token == Token.NULL)
				{
					token = Token.LITERAL1;
					literalChar = line.array[i];
					addToken(i - lastOffset,Token.NULL);
					lastOffset = i;
				}
				else if(token == Token.LITERAL1 && literalChar == line.array[i])
				{
					token = Token.NULL;
					literalChar = 0;
					addToken((i + 1) - lastOffset,Token.LITERAL1);
					lastOffset = i + 1;
				}
				break;
			default:
				break;
			}
		}
		if(token == Token.NULL)
			searchBack(line, length, false);
		if(lastOffset != length)
			addToken(length - lastOffset,token);
		return token;
	}

	// protected members
	protected boolean isTSQL = false;

	// private members
	private KeywordMap keywords;
	private char literalChar = 0;

	private void searchBack(Segment line, int pos)
	{
		searchBack(line, pos, true);
	}

	private void searchBack(Segment line, int pos, boolean padNull)
	{
		int len = pos - lastKeyword;
		byte id = keywords.lookup(line,lastKeyword,len);
		if(id != Token.NULL)
		{
			if(lastKeyword != lastOffset)
				addToken(lastKeyword - lastOffset,Token.NULL);
			addToken(len,id);
			lastOffset = pos;
		}
		lastKeyword = pos + 1;
		if (padNull && lastOffset < pos)
			addToken(pos - lastOffset, Token.NULL);
	}
}
