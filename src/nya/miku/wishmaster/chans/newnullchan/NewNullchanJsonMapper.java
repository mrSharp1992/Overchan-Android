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

package nya.miku.wishmaster.chans.newnullchan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import nya.miku.wishmaster.api.models.AttachmentModel;
import nya.miku.wishmaster.api.models.BoardModel;
import nya.miku.wishmaster.api.models.PostModel;
import nya.miku.wishmaster.api.models.UrlPageModel;
import nya.miku.wishmaster.api.util.RegexUtils;
import nya.miku.wishmaster.lib.org_json.JSONArray;
import nya.miku.wishmaster.lib.org_json.JSONObject;

public class NewNullchanJsonMapper {

    private static final String[] ATTACHMENT_FORMATS = new String[] { "jpg", "jpeg", "png", "gif" };

    private static final Pattern SPOILER_MARK_PATTERN = Pattern.compile("<mark>(.*?)</mark>");
    private static final Pattern BLOCKQUOTE_MARK_PATTERN = Pattern.compile("<blockquote>(.*?)</blockquote>");

    static BoardModel mapBoardModel(JSONObject object) {
        BoardModel model = getDefaultBoardModel(object.getString("dir"));
        model.boardDescription = object.optString("name", model.boardName);
        return model;
    }

    static BoardModel getDefaultBoardModel(String boardName) {
        BoardModel model = new BoardModel();
        model.chan = NewNullchanModule.CHAN_NAME;
        model.boardName = boardName;
        model.boardDescription = boardName;
        model.boardCategory = null;
        model.nsfw = false;
        model.uniqueAttachmentNames = true;
        model.timeZoneId = "GMT";
        model.defaultUserName = "Anonymous";
        model.bumpLimit = Integer.MAX_VALUE;
        model.readonlyBoard = false;
        model.requiredFileForNewThread = false;
        model.allowDeletePosts = false;
        model.allowDeleteFiles = false;
        model.allowReport = BoardModel.REPORT_NOT_ALLOWED;
        model.allowNames = false;
        model.allowSubjects = false;
        model.allowSage = false;
        model.allowEmails = false;
        model.allowCustomMark = false;
        model.allowRandomHash = true;
        model.allowIcons = false;
        model.attachmentsMaxCount = 8;
        model.attachmentsFormatFilters = ATTACHMENT_FORMATS;
        model.markType = BoardModel.MARK_NULL_CHAN;
        model.firstPage = 0;
        model.lastPage = BoardModel.LAST_PAGE_UNDEFINED;
        model.searchAllowed = false;
        model.catalogAllowed = false;
        return model;
    }

    static Map<String, List<String>> buildReplyMap(JSONArray posts, String opPost) {
        HashMap<String, List<String>> result = new HashMap<String, List<String>>();
        for (int i = 0; i < posts.length(); i++) {
            String postNumber = posts.getJSONObject(i).optString("id");
            if (opPost.equals(postNumber)) continue;
            JSONArray repliedByIds = posts.getJSONObject(i).getJSONArray("repliedByIds");
            for (int j = 0; j < repliedByIds.length(); j++) {
                String repliedNumber = Integer.valueOf(repliedByIds.optInt(j)).toString();
                if (!result.containsKey(repliedNumber))
                    result.put(repliedNumber, new ArrayList<String>());
                ArrayList<String> replies = (ArrayList<String>) result.get(repliedNumber);
                replies.add(postNumber);
            }
        }
        return result;
    }

    static String buildReplyUrl(String chanName, String boardName, String threadNumber, String postNumber, NewNullchanModule chan) {
        UrlPageModel pageModel = new UrlPageModel();
        pageModel.chanName = chanName;
        pageModel.type = UrlPageModel.TYPE_THREADPAGE;
        pageModel.boardName = boardName;
        pageModel.threadNumber = threadNumber;
        pageModel.postNumber = postNumber;
        return chan.buildUrl(pageModel);
    }

    static PostModel mapPostModel(JSONObject object, boolean useHttps, String board, NewNullchanModule chan, Map<String, List<String>> replyMap) {
        PostModel model = new PostModel();
        model.name = "";
        model.number = object.optString("id");
        model.comment = object.optString("messageHtml");
        model.op = object.optBoolean("isOpPost", false);
        model.deleted = object.optBoolean("isDeleted", false);
        model.timestamp = object.optLong("date") * 1000;
        model.parentThread = object.optString("threadId");
        List<String> replies = null;
        if (replyMap != null) replies = replyMap.get(model.number);

        JSONArray referencesToIds = object.getJSONArray("referencesToIds");
        for (int i = 0; i < referencesToIds.length(); i++) {
            String postNumber = Integer.valueOf(referencesToIds.optInt(i)).toString();
            String url = buildReplyUrl(chan.getChanName(), board, object.optString("threadId"), postNumber, chan);
            String referenceLink = String.format("<a href=\"%s\">&gt;&gt;%s</a>", url, postNumber);
            String reference = "&gt;&gt;" + postNumber;
            model.comment = model.comment.replaceAll(reference, referenceLink);
        }

        if (replies != null) {
            String references = "";
            for (int i = 0; i < replies.size(); i++) {
                String postNumber = replies.get(i);
                if (model.parentThread.equals(postNumber)) continue;
                String url = buildReplyUrl(chan.getChanName(), board, object.optString("threadId"), postNumber, chan);
                references += String.format("<a href=\"%s\">&gt;&gt;%s</a><br />", url, postNumber);
            }
            model.comment = references + model.comment;
        }
        model.comment = RegexUtils.replaceAll(model.comment, SPOILER_MARK_PATTERN, "<span class=\"spoiler\">$1</span>");
        model.comment = RegexUtils.replaceAll(model.comment, BLOCKQUOTE_MARK_PATTERN, "<span class=\"quote\">$1</span>");
        // TODO: convert content of <pre> to html
        JSONArray attachments = object.optJSONArray("attachments");
        if (attachments != null) {
            model.attachments = new AttachmentModel[attachments.length()];
            for (int i = 0; i < attachments.length(); i++) {
                AttachmentModel attachment = new AttachmentModel();
                JSONObject images = attachments.getJSONObject(i).getJSONObject("images");
                JSONObject original = images.getJSONObject("original");
                JSONObject thumb = images.getJSONObject("thumb_200px");
                boolean isNsfw = attachments.getJSONObject(i).optBoolean("isNsfw", false);
                attachment.originalName = original.optString("name");
                attachment.height = original.optInt("height", -1);
                attachment.width = original.optInt("width", -1);
                attachment.size = (int) original.optDouble("size_kb", -1.0);
                attachment.isSpoiler = isNsfw;
                attachment.path = original.optString("url");
                if (attachment.path.startsWith("//")) {
                    attachment.path = (useHttps ? "https:" : "http:") + attachment.path;
                }
                attachment.thumbnail = thumb.optString("url");
                if (attachment.thumbnail.startsWith("//")) {
                    attachment.thumbnail = (useHttps ? "https:" : "http:") + attachment.thumbnail;
                }
                
                model.attachments[i] = attachment;
            }
        }
        return model;
    }
    
}