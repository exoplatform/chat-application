package org.benjp.portlet.chat;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.xwiki.rendering.syntax.Syntax;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.logging.Logger;

@Named("wikiService")
@ApplicationScoped
public class WikiService {

  org.exoplatform.wiki.service.WikiService wikiService_;
  Logger log = Logger.getLogger("WikiService");

  @Inject
  public WikiService(org.exoplatform.wiki.service.WikiService wikiService)
  {
    wikiService_ = wikiService;
  }

  protected void createIntranetPage(String title, String content)
  {
    createOrEditPage("Meeting Notes", title, content, false, null);
  }

  /**
   *
   * @param title
   * @param content
   * @param spaceGroupId : format with spaces/space_group_name
   */
  protected void createSpacePage(String title, String content, String spaceGroupId)
  {
    createOrEditPage("Meeting Notes", title, content, false, spaceGroupId);
  }

  private void createOrEditPage(String parentTitle, String title, String content, boolean forceNew, String spaceGroupId)
  {
    String wikiType = PortalConfig.PORTAL_TYPE;
    String wikiOwner = "intranet";

    if (spaceGroupId != null)
    {
      wikiType = PortalConfig.GROUP_TYPE;
      wikiOwner = spaceGroupId;
    }

    try
    {
/*
      if (forceNew && !title.equals("Wiki Home"))
      {
        if (wikiService_.isExisting(wikiType, wikiOwner, TitleResolver.getId(title, false)))
        {
          wikiService_.deletePage(wikiType, wikiOwner, TitleResolver.getId(title, false));
        }
      }
*/
      if (!wikiService_.isExisting(wikiType, wikiOwner, TitleResolver.getId(parentTitle, false)))
      {
        PageImpl ppage = (PageImpl) wikiService_.createPage(wikiType, wikiOwner, parentTitle, TitleResolver.getId("Wiki Home", false));
        ppage.getContent().setText("= "+parentTitle+" =\n");
        ppage.setSyntax(Syntax.XWIKI_2_0.toIdString());
        ppage.checkin();
        ppage.checkout();
      }

      PageImpl page;
      if (wikiService_.isExisting(wikiType, wikiOwner, TitleResolver.getId(title, false)))
      {
        page = (PageImpl) wikiService_.getPageById(wikiType, wikiOwner, TitleResolver.getId(title, false));
      }
      else
      {
        page = (PageImpl) wikiService_.createPage(wikiType, wikiOwner, title, TitleResolver.getId(parentTitle, false));
      }

      page.getContent().setText(content);
      page.setSyntax(Syntax.XWIKI_2_0.toIdString());
      page.checkin();
      page.checkout();

    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }

  }

}
