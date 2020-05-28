package org.exoplatform.chat.service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import org.exoplatform.chat.services.ChatService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.IDType;

@Path("/chat/api/1.0/wiki/")
public class WikiService implements ResourceContainer {

  org.exoplatform.wiki.service.WikiService wikiService_;

  private static final Logger              LOG = Logger.getLogger("WikiService");

  public static final String               ANY = "any".intern();

  public WikiService(org.exoplatform.wiki.service.WikiService wikiService) {
    wikiService_ = wikiService;
  }

  @SuppressWarnings("unchecked")
  @POST
  @Path("/saveWiki")
  @RolesAllowed("users")
  public Response saveWiki(@Context HttpServletRequest request,
                           @Context SecurityContext sc,
                           @FormParam("targetFullname") String targetFullname,
                           @FormParam("content") String content) throws Exception {
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    String currentUser = sc.getUserPrincipal().getName();

    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH-mm");
    String group = null, title = null, path = "";
    JSONObject jsonObject = (JSONObject) JSONValue.parse(content);
    String typeRoom = (String) jsonObject.get("typeRoom");
    String xwiki = (String) jsonObject.get("xwiki");
    ArrayList<String> users = (ArrayList<String>) jsonObject.get("users");
    if (ChatService.TYPE_ROOM_SPACE.equalsIgnoreCase(typeRoom)) {
      Space spaceBean = spaceService.getSpaceByDisplayName(targetFullname);
      if (spaceBean != null) {
        group = spaceBean.getGroupId();
        if (group.startsWith("/"))
          group = group.substring(1);
        title = "Meeting " + sdf.format(new Date());
        path = createSpacePage(currentUser, title, xwiki, group, users);
      }
    } else {
      title = targetFullname + " Meeting " + sdf.format(new Date());
      path = createIntranetPage(currentUser, title, xwiki, users);
    }

    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
    JSONObject data = new JSONObject();
    data.put("status", "ok");
    data.put("path", path);

    return Response.ok(data.toString(), MediaType.APPLICATION_JSON).cacheControl(cacheControl).build();
  }

  protected String createIntranetPage(String creator, String title, String content, ArrayList<String> users) {
    return createOrEditPage(creator, "Meeting Notes", title, content, users, false, null);
  }

  /**
   * @param title
   * @param content
   * @param spaceGroupId : format with spaces/space_group_name
   */
  protected String createSpacePage(String creator, String title, String content, String spaceGroupId, ArrayList<String> users) {
    return createOrEditPage(creator, "Meeting Notes", title, content, users, false, spaceGroupId);
  }

  private String createOrEditPage(String creator,
                                  String parentTitle,
                                  String title,
                                  String content,
                                  ArrayList<String> users,
                                  boolean forceNew,
                                  String spaceGroupId) {
    String wikiType = PortalConfig.PORTAL_TYPE;
    String wikiOwner;

    if (spaceGroupId != null) {
      wikiType = PortalConfig.GROUP_TYPE;
      wikiOwner = "/" + spaceGroupId;
    } else {
      wikiOwner = CommonsUtils.getCurrentSite().getName();
    }

    String path = "";
    try {
      synchronized (wikiService_) {

        Page ppage = wikiService_.getPageOfWikiByName(wikiType, wikiOwner, TitleResolver.getId(parentTitle, false));
        if (ppage == null) {
          ppage = new Page();
          ppage.setTitle(parentTitle);
          ppage.setContent("<h1>" + parentTitle + "</h1>\n");
          ppage.setSyntax("xhtml/1.0");
          Wiki wiki = wikiService_.getWikiByTypeAndOwner(wikiType, wikiOwner);
          if (wiki == null) {
            wiki = wikiService_.createWiki(wikiType, wikiOwner);
          }
          Page wikiHome = wiki.getWikiHome();
          setPermissionForReportAsWiki(Collections.emptyList(), ppage, wikiHome);
          List<PermissionEntry> permissions = ppage.getPermissions();
          permissions.add(new PermissionEntry(ANY,
                                              "",
                                              IDType.USER,
                                              new Permission[] { new Permission(PermissionType.VIEWPAGE, true) }));
          ppage.setPermissions(permissions);
          Wiki pwiki = new Wiki();
          pwiki.setOwner(wikiOwner);
          pwiki.setType(wikiType);
          ppage = wikiService_.createPage(pwiki, "WikiHome", ppage);

          // remove permissions on the Meeting Notes parent page for current
          // user (automatically added by the Wiki API)
          permissions = ppage.getPermissions();
          for (int i = 0; i < permissions.size(); i++) {
            PermissionEntry permission = permissions.get(i);
            if (creator.equals(permission.getId())) {
              permissions.remove(i);
            }
          }
          ppage.setPermissions(permissions);
          wikiService_.updatePage(ppage, null);
        }

        Page page = new Page();
        page.setTitle(title);
        page.setContent(content);
        page.setSyntax("xhtml/1.0");
        setPermissionForReportAsWiki(users, page, ppage);
        page.setOwner(creator);
        page.setAuthor(creator);
        page.setMinorEdit(false);

        if (wikiType.equals(PortalConfig.GROUP_TYPE)) {
          // http://demo.exoplatform.net/portal/intranet/wiki/group/spaces/bank_project/Meeting_06-11-2013
          path = "/wiki/" + wikiType + wikiOwner + "/" + TitleResolver.getId(title, false);
        } else if (wikiType.equals(PortalConfig.PORTAL_TYPE)) {
          // http://demo.exoplatform.net/portal/intranet/wiki/Sales_Meetings_Meeting_06-11-2013
          path = "/" + PortalContainer.getInstance().getName() + "/" + wikiOwner + "/wiki/" + TitleResolver.getId(title, false);
        }
        page.setUrl(path);
        Wiki wiki = new Wiki();
        wiki.setOwner(wikiOwner);
        wiki.setType(wikiType);
        if (wikiService_.isExisting(wikiType, wikiOwner, TitleResolver.getId(title, false))) {
          wikiService_.getPageById(TitleResolver.getId(title, false));
        } else {
          try {
            wikiService_.createPage(wiki, TitleResolver.getId(parentTitle, false), page);
          } catch (Exception e) {
            wikiService_.getPageById(TitleResolver.getId(title, false));
          }
        }
      }
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Unknown exception", e);
    }

    return path;
  }

  public void setPermissionForReportAsWiki(List<String> users, Page page, Page parentPage) {
    try {
      Permission[] allPermissions = new Permission[] { new Permission(PermissionType.VIEWPAGE, true),
          new Permission(PermissionType.EDITPAGE, true), };
      List<PermissionEntry> permissions = parentPage.getPermissions();
      if (permissions != null) {
        // remove any permission
        int anyIndex = -1;
        for (int i = 0; i < permissions.size(); i++) {
          PermissionEntry any = permissions.get(i);
          if (ANY.equals(any.getId()))
            anyIndex = i;
        }
        if (anyIndex > -1)
          permissions.remove(anyIndex);
        for (int i = 0; i < users.size(); i++) {
          String strUser = users.get(i).toString();
          PermissionEntry userPermission = new PermissionEntry(strUser, strUser, IDType.USER, allPermissions);
          permissions.add(userPermission);
        }
        page.setPermissions(permissions);
      }

    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Unknown exception", e);

    }
  }
}
