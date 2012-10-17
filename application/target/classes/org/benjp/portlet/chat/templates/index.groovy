;out.print(Constants.s0);
;out.print("${user}");
;out.print(Constants.s1);
;out.print("${sessionId}");
;out.print(Constants.s2);
;out.print("${room}");
;out.print(Constants.s3);

public static class Constants
{
public static final juzu.io.CharArray$Simple s0 = new juzu.io.CharArray$Simple('<div class="chatapplication">\n  <div id="whoisonline"></div>\n  <div class="rightchat" style="display:none;">\n    <div id="chats"></div>\n    <div class="chatmessage">\n      <input type="text" name="text" id="msg" autocomplete="off" />\n    </div>\n  </div>\n</div>\n\n<script>\n  var username = "');
public static final juzu.io.CharArray$Simple s1 = new juzu.io.CharArray$Simple('";\n  var sessionId = "');
public static final juzu.io.CharArray$Simple s2 = new juzu.io.CharArray$Simple('";\n  var jzChatWhoIsOnline = "/chatServer/whoIsOnline";\n  var jzChatSend = "/chatServer/send";\n  var jzChatGetRoom = "/chatServer/getRoom";\n  var jzChatUpdateUnreadMessages = "/chatServer/updateUnreadMessages";\n  var room = "');
public static final juzu.io.CharArray$Simple s3 = new juzu.io.CharArray$Simple('";\n  var old = \'\';\n  var chatEventSource;\n  var targetUser;\n</script>\n');
public static final Map<Integer, juzu.impl.template.spi.juzu.dialect.gtmpl.Foo> TABLE = [
2:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(19,12),'user'),
4:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(20,13),'sessionId'),
6:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(15,18),'room')];
}
