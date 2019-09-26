/*
 * Overchan Android (Meta Imageboard Client)
 * Copyright (C) 2014-2016  miku-nyan <https://github.com/miku-nyan>
 *     
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nya.miku.wishmaster.chans.kohlchan;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nya.miku.wishmaster.api.models.SimpleBoardModel;
import nya.miku.wishmaster.api.util.RegexUtils;

public class KohlBoardsListReader implements Closeable {
    private static final List<String> NSFW_BOARDS = Arrays.asList(new String[] { "b", "int", "s" });
    
    private final Reader _in;
    private StringBuilder readBuffer = new StringBuilder();
    private String currentCategory;
    private List<SimpleBoardModel> boards;
    
    private static final int FILTER_CATEGORY = 0;
    private static final int FILTER_BOARD = 1;
    private static final char[][] FILTERS = {
        "<h2".toCharArray(),
        "<li".toCharArray()
    };
    
    private static final char[] CLOSE = ">".toCharArray();
    private static final char[] LI_CLOSE = "</li>".toCharArray();
    private static final char[] LEGEND_CLOSE = "</h2>".toCharArray();
    
    private static final Pattern BOARD_PATTERN = Pattern.compile("/(\\w+)/(?:\\s+-\\s+(.*))?", Pattern.DOTALL);
    
    public KohlBoardsListReader(Reader reader) {
        _in = reader;
    }
    
    public KohlBoardsListReader(InputStream in) {
        this(new BufferedReader(new InputStreamReader(in)));
    }
    
    public SimpleBoardModel[] readBoardsList() throws IOException {
        boards = new ArrayList<SimpleBoardModel>();
        
        int filtersCount = FILTERS.length;
        int[] pos = new int[filtersCount];
        int[] len = new int[filtersCount];
        for (int i=0; i<filtersCount; ++i) len[i] = FILTERS[i].length;
        
        int curChar;
        while ((curChar = _in.read()) != -1) {
            for (int i=0; i<filtersCount; ++i) {
                if (curChar == FILTERS[i][pos[i]]) {
                    ++pos[i];
                    if (pos[i] == len[i]) {
                        handleFilter(i);
                        pos[i] = 0;
                    }
                } else {
                    if (pos[i] != 0) pos[i] = curChar == FILTERS[i][0] ? 1 : 0;
                }
            }
        }
        return boards.toArray(new SimpleBoardModel[boards.size()]);
    }
    
    private void handleFilter(int filter) throws IOException {
        switch (filter) {
            case FILTER_CATEGORY:
                skipUntilSequence(CLOSE);
                String cat = readUntilSequence(LEGEND_CLOSE);
                currentCategory = StringEscapeUtils.unescapeHtml4(cat);
                break;
            case FILTER_BOARD:
                skipUntilSequence(CLOSE);
                String board = RegexUtils.removeHtmlTags(readUntilSequence(LI_CLOSE)).trim();
                Matcher boardMatcher = BOARD_PATTERN.matcher(board);
                if (boardMatcher.matches()) {
                    SimpleBoardModel model = new SimpleBoardModel();
                    model.chan = KohlchanModule.CHAN_NAME;
                    model.boardName = boardMatcher.group(1);
                    model.boardDescription = boardMatcher.group(2) != null ? 
                            StringEscapeUtils.unescapeHtml4(boardMatcher.group(2)) :
                            model.boardName;
                    model.boardCategory = currentCategory;
                    model.nsfw = NSFW_BOARDS.indexOf(model.boardName) != -1;
                    boards.add(model);
                }
        }
    }

    private void skipUntilSequence(char[] sequence) throws IOException {
        int len = sequence.length;
        if (len == 0) return;
        int pos = 0;
        int curChar;
        while ((curChar = _in.read()) != -1) {
            if (curChar == sequence[pos]) {
                ++pos;
                if (pos == len) break;
            } else {
                if (pos != 0) pos = curChar == sequence[0] ? 1 : 0;
            }
        }
    }
    
    private String readUntilSequence(char[] sequence) throws IOException {
        int len = sequence.length;
        if (len == 0) return "";
        readBuffer.setLength(0);
        int pos = 0;
        int curChar;
        while ((curChar = _in.read()) != -1) {
            readBuffer.append((char) curChar);
            if (curChar == sequence[pos]) {
                ++pos;
                if (pos == len) break;
            } else {
                if (pos != 0) pos = curChar == sequence[0] ? 1 : 0;
            }
        }
        int buflen = readBuffer.length();
        if (buflen >= len) {
            readBuffer.setLength(buflen - len);
            return readBuffer.toString();
        } else {
            return "";
        }
    }
    
    @Override
    public void close() throws IOException {
        _in.close();
    }
    
}
