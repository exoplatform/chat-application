package org.exoplatform.chat.portlet.chat;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.PermissionEntry;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.mow.api.Permission;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.IDType;
import org.xwiki.rendering.syntax.Syntax;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("wikiService")
@ApplicationScoped
public class WikiService {

  org.exoplatform.wiki.service.WikiService wikiService_;
  private static final Logger LOG = Logger.getLogger("WikiService");
  
  public static final String ANY = "any".intern();

  @Inject
  public WikiService(org.exoplatform.wiki.service.WikiService wikiService)
  {
    wikiService_ = wikiService;
  }

  protected String createIntranetPage(String creator, String title, String content, ArrayList<String> users)
  {
    return createOrEditPage(creator, "Meeting Notes", title, content, users, false, null);
  }

  /**
   *
   * @param title
   * @param content
   * @param spaceGroupId : format with spaces/space_group_name
   */
  protected String createSpacePage(String creator, String title, String content, String spaceGroupId, ArrayList<String> users)
  {
    return createOrEditPage(creator, "Meeting Notes", title, content, users, false, spaceGroupId);
  }

  private String createOrEditPage(String creator, String parentTitle, String title, String content, ArrayList<String> users, boolean forceNew, String spaceGroupId)
  {
    String wikiType = PortalConfig.PORTAL_TYPE;
    String wikiOwner = "intranet";
    String path = "";

    if (spaceGroupId != null)
    {
      wikiType = PortalConfig.GROUP_TYPE;
      wikiOwner = "/" + spaceGroupId;
    }

    try
    {
      synchronized(wikiService_) {

        Page ppage = wikiService_.getPageOfWikiByName(wikiType, wikiOwner, TitleResolver.getId(parentTitle, false));
        if (ppage == null) {
          ppage = new Page();
          ppage.setTitle(parentTitle);
          ppage.setContent("= " + parentTitle + " =\n");
          ppage.setSyntax(Syntax.XWIKI_2_0.toIdString());
          ppage.setOwner(creator);
          ppage.setAuthor(creator);
          Wiki wiki = wikiService_.getWikiByTypeAndOwner(wikiType, wikiOwner);
          if(wiki == null) {
            wiki = wikiService_.createWiki(wikiType, wikiOwner);
          }
          Page wikiHome = wiki.getWikiHome();
          setPermissionForReportAsWiki(Collections.EMPTY_LIST, ppage, wikiHome);
          List<PermissionEntry> permissions = ppage.getPermissions();
          permissions.add(new PermissionEntry(ANY,"", IDType.USER, new Permission[]{new Permission (PermissionType.VIEWPAGE, true)}));
          ppage.setPermissions(permissions);
          Wiki pwiki = new Wiki();
          pwiki.setOwner(wikiOwner);
          pwiki.setType(wikiType);
          ppage = wikiService_.createPage(pwiki, "WikiHome", ppage);
        }

        Page page = new Page();
        page.setTitle(title);
        page.setContent(content);
        page.setSyntax(Syntax.XWIKI_2_0.toIdString());
        setPermissionForReportAsWiki(users, page, ppage);
        page.setOwner(creator);
        page.setAuthor(creator);
        page.setMinorEdit(false);
        if (wikiType.equals(PortalConfig.GROUP_TYPE)) {
            // http://demo.exoplatform.net/portal/intranet/wiki/group/spaces/bank_project/Meeting_06-11-2013
            path = "/portal/intranet/wiki/" + wikiType + wikiOwner + "/" + TitleResolver.getId(title, false);
          } else if (wikiType.equals(PortalConfig.PORTAL_TYPE)) {
            // http://demo.exoplatform.net/portal/intranet/wiki/Sales_Meetings_Meeting_06-11-2013
            path = "/portal/intranet/wiki/" + TitleResolver.getId(title, false);
          }
        page.setUrl(path);
        Wiki wiki = new Wiki();
        wiki.setOwner(wikiOwner);
        wiki.setType(wikiType);
        boolean isPageExisted = false;
        if (wikiService_.isExisting(wikiType, wikiOwner, TitleResolver.getId(title, false))) {
          wikiService_.getPageById(TitleResolver.getId(title, false));
          isPageExisted = true;
        } else {
          try {
            wikiService_.createPage(wiki, TitleResolver.getId(parentTitle, false), page);
          } catch (Exception e) {
            isPageExisted = true;
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
      Permission[] allPermissions = new Permission[] {
              new Permission(PermissionType.VIEWPAGE, true),
              new Permission(PermissionType.EDITPAGE, true),
      };
      List<PermissionEntry> permissions = parentPage.getPermissions();
      if (permissions != null) {
      // remove any permission
        int anyIndex = -1;
        for (int i = 0; i < permissions.size(); i ++) {
          PermissionEntry any = permissions.get(i);
          if (ANY.equals(any.getId())) anyIndex = i;
        }
        if (anyIndex > -1 ) permissions.remove(anyIndex);
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
