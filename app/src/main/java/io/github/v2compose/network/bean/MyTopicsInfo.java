package io.github.v2compose.network.bean;

import java.io.Serializable;
import java.util.List;

import io.github.v2compose.util.AvatarUtils;
import io.github.v2compose.util.Check;
import io.github.v2compose.util.Utils;
import me.ghui.fruit.Attrs;
import me.ghui.fruit.annotations.Pick;

/**
 * Created by ghui on 17/05/2017.
 * https://www.v2ex.com/my/topics
 */

@Pick("div#Wrapper")
public class MyTopicsInfo extends BaseInfo {
    @Pick(value = "input.page_input", attr = "max")
    private String total;
    @Pick("div.cell.item")
    private List<Item> items;

    public List<Item> getItems() {
        return items;
    }

    public int getTotal() {
        try {
            return Integer.parseInt(total);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }


    @Override
    public String toString() {
        return "TopicStarInfo{" +
                "total=" + total +
                ", items=" + items +
                '}';
    }

    @Override
    public boolean isValid() {
        if (Utils.listSize(items) <= 0) return true;
        return Check.notEmpty(items.get(0).title);
    }

    public static class Item implements Serializable {
        @Pick(value = "td>a[href^=/member]", attr = Attrs.HREF)
        private String userLink;
        @Pick(value = "img.avatar", attr = Attrs.SRC)
        private String avatar;
        @Pick("span.item_title")
        private String title;
        @Pick(value = "span.item_title a", attr = Attrs.HREF)
        private String link;
        @Pick("a[class^=count_]")
        private int commentNum;
        @Pick("a.node")
        private String tagTitle;
        @Pick(value = "a.node", attr = Attrs.HREF)
        private String tagLink;
        @Pick(value = "span.small.fade", attr = Attrs.OWN_TEXT)
        private String time;

        @Override
        public String toString() {
            return "Item{" +
                    "userLink='" + userLink + '\'' +
                    "userName='" + getUserName() + '\'' +
                    ", avatar='" + avatar + '\'' +
                    ", title='" + title + '\'' +
                    ", link='" + link + '\'' +
                    ", commentNum=" + commentNum +
                    ", tag='" + tagTitle + '\'' +
                    ", tagLink='" + tagLink + '\'' +
                    '}';
        }

        private String _id;

        public String getId() {
            if (_id != null) return _id;
            if (link == null) return "";
            _id = link.substring("/t/".length(), link.indexOf('#'));
            return _id;
        }

        private String _time;

        public String getTime() {
            if (_time != null) return _time;
            //   • •  36 天前  •  最后回复来自
            if (Check.isEmpty(time) || !time.contains("前")) return "";
            time = time.replaceAll(" ", "");
            int endIndex = time.indexOf("前");
            int startIndex = 0;
            for (int i = endIndex - 1; i >= 0; i--) {
                if (time.charAt(i) == '•') {
                    startIndex = i;
                    break;
                }
            }
            _time = time.substring(startIndex + 1, endIndex + 1).trim();
            return _time;
        }

        private String _userName;

        public String getUserName() {
            if (_userName != null) return _userName;
            if (Check.isEmpty(userLink)) return null;
            _userName = userLink.substring(userLink.lastIndexOf("/") + 1);
            return _userName;
        }

        public String getUserLink() {
            return userLink;
        }

        private String _avatar;

        public String getAvatar() {
            if (_avatar != null) return _avatar;
            if (avatar == null) return "";
            _avatar = AvatarUtils.adjustAvatar(avatar);
            return _avatar;
        }

        public String getTitle() {
            return title;
        }

        public String getLink() {
            return link;
        }

        public int getCommentNum() {
            return commentNum;
        }

        private String _tagName;

        public String getTagName() {
            if (_tagName != null) return _tagName;
            _tagName = tagLink.substring("/go/".length());
            return _tagName;
        }

        public String getTagTitle() {
            return tagTitle;
        }
    }
}
