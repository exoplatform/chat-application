package org.benjp.portlet.chat.templates;
import juzu.impl.plugin.template.metadata.TemplateDescriptor;
import juzu.impl.plugin.template.TemplatePlugin;
@javax.annotation.Generated({})
@juzu.Path("users.gtmpl")
public class users extends juzu.template.Template
{
@javax.inject.Inject
public users(TemplatePlugin templatePlugin)
{
super(templatePlugin, "users.gtmpl");
}
public static final juzu.impl.plugin.template.metadata.TemplateDescriptor DESCRIPTOR = new juzu.impl.plugin.template.metadata.TemplateDescriptor(org.benjp.portlet.chat.templates.users.class);
public Builder with() {
return new Builder();
}
public class Builder extends juzu.template.Template.Builder
{
}
}
