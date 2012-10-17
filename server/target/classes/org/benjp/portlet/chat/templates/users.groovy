;out.print(Constants.s0);
 if (rooms!=null) { 
;out.print(Constants.s1);
 rooms.each { room -> 
;out.print(Constants.s2);
;out.print("${room.user}");
;out.print(Constants.s3);
;out.print("${room.user}");
;out.print(Constants.s4);
;out.print("${room.room}");
;out.print(Constants.s5);
;out.print("${room.user}");
;out.print(Constants.s6);
 if (room.unreadTotal>0) { 
;out.print(Constants.s7);
;out.print("${room.unreadTotal}");
;out.print(Constants.s8);
 } 
;out.print(Constants.s9);
 } 
;out.print(Constants.s10);
 } 
;out.print(Constants.s11);

public static class Constants
{
public static final juzu.io.CharArray$Simple s0 = new juzu.io.CharArray$Simple('<table class="table">\n\n  ');
public static final juzu.io.CharArray$Simple s1 = new juzu.io.CharArray$Simple('\n    ');
public static final juzu.io.CharArray$Simple s2 = new juzu.io.CharArray$Simple('\n    <tr id="users-online-');
public static final juzu.io.CharArray$Simple s3 = new juzu.io.CharArray$Simple('" class="users-online">\n      <td>\n        <a href="#" user-data="');
public static final juzu.io.CharArray$Simple s4 = new juzu.io.CharArray$Simple('" room-data="');
public static final juzu.io.CharArray$Simple s5 = new juzu.io.CharArray$Simple('" class="user-link">');
public static final juzu.io.CharArray$Simple s6 = new juzu.io.CharArray$Simple('</a>\n      </td>\n      <td>\n        ');
public static final juzu.io.CharArray$Simple s7 = new juzu.io.CharArray$Simple('\n        <span class="badge badge-info" style="float:right;">');
public static final juzu.io.CharArray$Simple s8 = new juzu.io.CharArray$Simple('</span>\n        ');
public static final juzu.io.CharArray$Simple s9 = new juzu.io.CharArray$Simple('\n      </td>\n    </tr>\n\n    ');
public static final juzu.io.CharArray$Simple s10 = new juzu.io.CharArray$Simple('\n  ');
public static final juzu.io.CharArray$Simple s11 = new juzu.io.CharArray$Simple('\n</table>\n\n<script>\n  $("#users-online-"+targetUser).addClass("info");\n\n  $(\'.user-link\').on("click", function() {\n    targetUser = $(this).attr("user-data");\n    //console.log("TARGET::"+targetUser);\n    $(".users-online").removeClass("info");\n    $("#users-online-"+targetUser).addClass("info");\n\n    $.ajax({\n      url: jzChatGetRoom,\n      data: {"targetUser": targetUser,\n              "user": username,\n              "sessionId": sessionId\n              },\n\n      success: function(response){\n        console.log("SUCCESS::getRoom::"+response);\n        room = response;\n\n        if (chatEventSource!=undefined) chatEventSource.close();\n        chatEventSource = new EventSource(jzChatSend+\'?room=\'+room);\n\n        chatEventSource.onmessage = function(e){\n          console.log("chatEventSource::onmessage::");\n          if(old!=e.data){\n            //console.log("DATA="+e.data);\n            $(".rightchat").css("display", "block");\n            $("#chats").html(\'<span>\'+e.data+\'</span>\');\n            $("#chats").animate({ scrollTop: 2000 }, \'normal\');\n            old = e.data;\n          }\n        };\n\n      },\n\n      error: function(xhr, status, error){\n        console.log("ERROR::"+xhr.responseText);\n      }\n\n    });\n\n\n\n  });\n\n\n</script>\n');
public static final Map<Integer, juzu.impl.template.spi.juzu.dialect.gtmpl.Foo> TABLE = [
16:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(61,11),'room.unreadTotal'),
2:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(3,3),' if (rooms!=null) { '),
18:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(9,12),' } '),
4:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(5,4),' rooms.each { room -> '),
20:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(5,16),' } '),
6:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(26,5),'room.user'),
22:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(3,17),' } '),
8:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(32,7),'room.user'),
10:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(59,7),'room.room'),
12:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(93,7),'room.user'),
14:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(9,10),' if (room.unreadTotal>0) { ')];
}
