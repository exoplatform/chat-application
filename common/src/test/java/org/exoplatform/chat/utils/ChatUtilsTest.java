package org.exoplatform.chat.utils;

import org.exoplatform.chat.model.SpaceBean;
import org.exoplatform.chat.model.SpaceBeans;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;


public class ChatUtilsTest {

    @Test
    public void testFromString() throws IOException, ClassNotFoundException {
        SpaceBean space = new SpaceBean();
        space.setDisplayName("space 1");
        space.setGroupId("/spaces/space_1");
        space.setId("1");
        space.setShortName("space_1");
        space.setPrettyName("space_1");
        space.setTimestamp(-1);
        ArrayList<SpaceBean> spaces = new ArrayList();
        spaces.add(space);
        SpaceBeans beans = new SpaceBeans(spaces);
        String input ="rO0ABXNyACVvcmcuZXhvcGxhdGZvcm0uY2hhdC5tb2RlbC5TcGFjZUJlYW5zUs3d0MH0nzYCAAFMAAdzcGFjZXNfdAAVTGphdmEvdXRpbC9BcnJheUxpc3Q7eHBzcgATamF2YS51dGlsLkFycmF5TGlzdHiB0h2Zx2GdAwABSQAEc2l6ZXhwAAAAAXcEAAAAAXNyACRvcmcuZXhvcGxhdGZvcm0uY2hhdC5tb2RlbC5TcGFjZUJlYW7cTcoLVb0cWwIACVoADm1lZXRpbmdTdGFydGVkSgAJdGltZXN0YW1wTAALZGlzcGxheU5hbWV0ABJMamF2YS9sYW5nL1N0cmluZztMAAdncm91cElkcQB+AAZMAAJpZHEAfgAGTAAKcHJldHR5TmFtZXEAfgAGTAAEcm9vbXEAfgAGTAAJc2hvcnROYW1lcQB+AAZMAAlzdGFydFRpbWVxAH4ABnhwAP//////////dAAHc3BhY2UgMXQADy9zcGFjZXMvc3BhY2VfMXQAATF0AAdzcGFjZV8xcHEAfgALdAAAeA==";
        SpaceBeans outputObj = (SpaceBeans) ChatUtils.fromString(input);
        Assert.assertEquals(beans.getSpaces(),outputObj.getSpaces());
    }

    @Test
    public void testToString() throws IOException {
        SpaceBean space = new SpaceBean();
        space.setDisplayName("space 1");
        space.setGroupId("/spaces/space_1");
        space.setId("1");
        space.setShortName("space_1");
        space.setPrettyName("space_1");
        space.setTimestamp(-1);
        List<SpaceBean> spaces = new ArrayList();
        spaces.add(space);
        SpaceBeans beans = new SpaceBeans((ArrayList<SpaceBean>) spaces);
        String outputObj = ChatUtils.toString(beans);
        String expected= "rO0ABXNyACVvcmcuZXhvcGxhdGZvcm0uY2hhdC5tb2RlbC5TcGFjZUJlYW5zUs3d0MH0nzYCAAFMAAdzcGFjZXNfdAAVTGphdmEvdXRpbC9BcnJheUxpc3Q7eHBzcgATamF2YS51dGlsLkFycmF5TGlzdHiB0h2Zx2GdAwABSQAEc2l6ZXhwAAAAAXcEAAAAAXNyACRvcmcuZXhvcGxhdGZvcm0uY2hhdC5tb2RlbC5TcGFjZUJlYW7cTcoLVb0cWwIACVoADm1lZXRpbmdTdGFydGVkSgAJdGltZXN0YW1wTAALZGlzcGxheU5hbWV0ABJMamF2YS9sYW5nL1N0cmluZztMAAdncm91cElkcQB+AAZMAAJpZHEAfgAGTAAKcHJldHR5TmFtZXEAfgAGTAAEcm9vbXEAfgAGTAAJc2hvcnROYW1lcQB+AAZMAAlzdGFydFRpbWVxAH4ABnhwAP//////////dAAHc3BhY2UgMXQADy9zcGFjZXMvc3BhY2VfMXQAATF0AAdzcGFjZV8xcHEAfgALdAAAeA==";
        Assert.assertEquals(expected,outputObj);
    }
    @Test
    public void testGetRoomIdWithRoomNameAndUser(){
     String user = "user";
     String roomName = "room";
     String expected = "9decec739237f5ff6f0cc01944fd7cae5eb46f97";
     String roomID = ChatUtils.getRoomId(roomName,user);
     Assert.assertEquals(expected,roomID);
    }
    @Test
    public void testGetRoomIdWithRoomName(){
     String roomName = "room";
     String expected = "27442e996a1f3699be82deb2502bd8e8e7a3c532";
     String roomID = ChatUtils.getRoomId(roomName);
     Assert.assertEquals(expected,roomID);
    }

    @Test
    public void testGetExternalRoomId(){
     String roomName = "room";
     String expected = "9f8d540666d9623830d6e07981779df595333f23";
     String roomID = ChatUtils.getExternalRoomId(roomName);
     Assert.assertEquals(expected,roomID);
    }


}
