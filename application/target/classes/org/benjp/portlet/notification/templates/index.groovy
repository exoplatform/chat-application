;out.print(Constants.s0);
;out.print("${user}");
;out.print(Constants.s1);
;out.print("${lastRead}");
;out.print(Constants.s2);

public static class Constants
{
public static final juzu.io.CharArray$Simple s0 = new juzu.io.CharArray$Simple('<div id="chatnotification" style="display:none"></div>\n\n\n<script>\n  var username = "');
public static final juzu.io.CharArray$Simple s1 = new juzu.io.CharArray$Simple('";\n  var lastRead = "');
public static final juzu.io.CharArray$Simple s2 = new juzu.io.CharArray$Simple('";\n  var jzNotification = "/chatServer/notification";\n  var jzReadNotification = "/chatServer/readNotification";\n</script>\n');
public static final Map<Integer, juzu.impl.template.spi.juzu.dialect.gtmpl.Foo> TABLE = [
2:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(19,5),'user'),
4:new juzu.impl.template.spi.juzu.dialect.gtmpl.Foo(new juzu.impl.common.Location(19,6),'lastRead')];
}
