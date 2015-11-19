package org.exoplatform.chat.portlet.chat;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.PermissionEntry;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.mow.api.Permission;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.xwiki.rendering.syntax.Syntax;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
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

  protected String createIntranetPage(String title, String content, ArrayList<String> users)
  {
    return createOrEditPage("Meeting Notes", title, content, users, false, null);
  }

  /**
   *
   * @param title
   * @param content
   * @param spaceGroupId : format with spaces/space_group_name
   */
  protected String createSpacePage(String title, String content, String spaceGroupId, ArrayList<String> users)
  {
    return createOrEditPage("Meeting Notes", title, content, users, false, spaceGroupId);
  }

  private String createOrEditPage(String parentTitle, String title, String content, ArrayList<String> users, boolean forceNew, String spaceGroupId)
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

        if (!wikiService_.isExisting(wikiType, wikiOwner, TitleResolver.getId(parentTitle, false))) {
          Page ppage = new Page();
          ppage.setTitle(parentTitle);
          Wiki pwiki = new Wiki();
          pwiki.setOwner(wikiOwner);
          pwiki.setType(wikiType);
          Page createdPpage = wikiService_.createPage(pwiki, "WikiHome", ppage);
          createdPpage.setContent("= " + parentTitle + " =\n");
          createdPpage.setSyntax(Syntax.XWIKI_2_0.toIdString());
          wikiService_.createVersionOfPage(createdPpage);
        }

        Page page = new Page();
        page.setTitle(title);
        Wiki wiki = new Wiki();
        wiki.setOwner(wikiOwner);
        wiki.setType(wikiType);
        Page createdPage;
        boolean isPageExisted = false;
        if (wikiService_.isExisting(wikiType, wikiOwner, TitleResolver.getId(title, false))) {
          createdPage = wikiService_.getPageById(TitleResolver.getId(title, false));
          isPageExisted = true;
        } else {
          try {
            createdPage =  wikiService_.createPage(wiki, parentTitle, page);
          } catch (Exception e) {
            isPageExisted = true;
            createdPage = wikiService_.getPageById(TitleResolver.getId(title, false));
          }
        }

        createdPage.setContent(content);
        setPermissionForReportAsWiki(users, createdPage);
        createdPage.setSyntax(Syntax.XWIKI_2_0.toIdString());
        createdPage.setMinorEdit(false);
        wikiService_.createVersionOfPage(createdPage);

        if (wikiType.equals(PortalConfig.GROUP_TYPE)) {
          // http://demo.exoplatform.net/portal/intranet/wiki/group/spaces/bank_project/Meeting_06-11-2013
          path = "/portal/intranet/wiki/" + wikiType + wikiOwner + "/" + createdPage.getName();
        } else if (wikiType.equals(PortalConfig.PORTAL_TYPE)) {
          // http://demo.exoplatform.net/portal/intranet/wiki/Sales_Meetings_Meeting_06-11-2013
          path = "/portal/intranet/wiki/" + createdPage.getName();
        }
        createdPage.setUrl(path);
       }
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Unknown exception", e);
    }

    return path;
  }
  public void setPermissionForReportAsWiki(List<String> users, Page page) {
    try {
      Permission[] allPermissions = new Permission[] {
              new Permission(PermissionType.VIEWPAGE, true),
              new Permission(PermissionType.EDITPAGE, true),
      };
      List<PermissionEntry> permissions = page.getPermissions();
      int anyIndex = -1;
      for (int i = 0; i < permissions.size(); i ++) {
        PermissionEntry any = permissions.get(i);
        if (ANY.equals(any.getFullName())) anyIndex = i;
      }
      if (anyIndex > -1 ) permissions.remove(anyIndex);
      for (int i = 0; i < users.size(); i++) {
        PermissionEntry userPermission = new PermissionEntry();
        userPermission.setFullName(users.get(i).toString());
        userPermission.setPermissions(allPermissions);
        permissions.add(userPermission);
      }
      page.setPermissions(permissions);
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Unknown exception", e);

    }
  }
}
