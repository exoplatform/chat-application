package org.benjp.portlet.notification.templates;
import juzu.impl.plugin.template.metadata.TemplateDescriptor;
import juzu.impl.plugin.template.TemplatePlugin;
@javax.annotation.Generated({})
@juzu.Path("index.gtmpl")
public class index extends juzu.template.Template
{
@javax.inject.Inject
public index(TemplatePlugin templatePlugin)
{
super(templatePlugin, "index.gtmpl");
}
public static final juzu.impl.plugin.template.metadata.TemplateDescriptor DESCRIPTOR = new juzu.impl.plugin.template.metadata.TemplateDescriptor(org.benjp.portlet.notification.templates.index.class);
public Builder with() {
return new Builder();
}
public class Builder extends juzu.template.Template.Builder
{
}
}
