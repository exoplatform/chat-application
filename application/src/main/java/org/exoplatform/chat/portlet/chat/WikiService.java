package org.exoplatform.chat.portlet.chat;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
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
          PageImpl ppage = (PageImpl) wikiService_.createPage(wikiType, wikiOwner, parentTitle, TitleResolver.getId("Wiki Home", false));
          ppage.getContent().setText("= " + parentTitle + " =\n");
          ppage.setSyntax(Syntax.XWIKI_2_0.toIdString());
          ppage.checkin();
          ppage.checkout();
        }

        PageImpl page;
        boolean isPageExisted = false;
        if (wikiService_.isExisting(wikiType, wikiOwner, TitleResolver.getId(title, false))) {
          page = (PageImpl) wikiService_.getPageById(wikiType, wikiOwner, TitleResolver.getId(title, false));
          isPageExisted = true;
        } else {
          try {
            page = (PageImpl) wikiService_.createPage(wikiType, wikiOwner, title, TitleResolver.getId(parentTitle, false));

          } catch (Exception e) {
            isPageExisted = true;
            page = (PageImpl) wikiService_.getPageById(wikiType, wikiOwner, TitleResolver.getId(title, false));
          }
        }

        page.getContent().setText(content);
        setPermissionForReportAsWiki(users, page);
        page.setSyntax(Syntax.XWIKI_2_0.toIdString());
        page.setMinorEdit(false);
        page.checkin();
        page.checkout();

        if (wikiType.equals(PortalConfig.GROUP_TYPE)) {
          // http://demo.exoplatform.net/portal/intranet/wiki/group/spaces/bank_project/Meeting_06-11-2013
          path = "/portal/intranet/wiki/" + wikiType + wikiOwner + "/" + page.getName();
        } else if (wikiType.equals(PortalConfig.PORTAL_TYPE)) {
          // http://demo.exoplatform.net/portal/intranet/wiki/Sales_Meetings_Meeting_06-11-2013
          path = "/portal/intranet/wiki/" + page.getName();
        }
        page.setURL(path);
        //Post Activity
        if (!isPageExisted) {
          wikiService_.postAddPage(wikiType, wikiOwner, TitleResolver.getId(title, false), page);
        }
      }
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Unknown exception", e);
    }

    return path;
  }
  public void setPermissionForReportAsWiki(List<String> users, PageImpl page) {
    try {
      HashMap<String, String[]> permissions = page.getPermission();
      permissions.remove(ANY);
      for (int i = 0; i < users.size(); i++) {
        permissions.put(users.get(i).toString(),org.exoplatform.services.jcr.access.PermissionType.ALL);
      }
      page.setPermission(permissions);
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Unknown exception", e);

    }
  }
}
