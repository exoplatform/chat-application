/**
 * WARNING: This file is a copy from the original file existing on gatein-portal, it used only for unit tests 
 * 
 * 
 * Extension Registry API
 * This Javascript API is for override and extension mechanism of front-end components
 * 
 * Available methods:
 *  
 *  - registerComponent(app, componentName, componentOptions)       : register a component 
 *                                                                    params : - app: the application name
 *                                                                             - componentName: the component name
 *                                                                             - componentOptions: the component options
 *  
 *  - loadComponents(app)                                            : return registred components an application
 *                                                                     params  - app: the application name
 * 
 *  - registerExtension(app, extensionType, extensionContent)       : register an extension 
 *                                                                    params : - app: the application name
 *                                                                             - extensionType: the extension type (an application can have several types of extensions)
 *                                                                             - extensionContent: the extension content (structre and properties of the extension content is provided by the extension type)
 *  
 *  - loadExtensions(app, extensionType)                             : return registred extensions for an extension type of an application
 *                                                                     params  - app: the application name
 *                                                                             - extensionType: the extension type (an application can have several types of extensions)
 * 
 * How to use:
 *    The Extension Registry API is exposed by an AMD module with requireJS:
 *    - For Application developpement: adding "ExtensionRegistry" as module dependency in gatein-ressources.xml
 *    - For Extension developpement: adding "ExtensionRegistry" as module dependency using "requirejs" method
 */
(function(){
  
  var registry = [];
  
  function Module(moduleName) {
    this.moduleName = moduleName;
    this.components = [];
    this.extensions = [];
  }
  
  function Component(name, options) {
    this.componentName = name;
    this.componentOptions = options;
  }
  
  function Extension(name, content) {
    this.extensionType = name;
    this.extensionContent = content;
  }
  
  function registerComponent(app, componentName, componentOptions) {
    var module = findModule(app, true);
    
    var component = new Component(componentName, componentOptions);
    module.components.push(component);
  }
  
  function registerExtension(app, extensionType, extensionContent) {
    var module = findModule(app, true);
    
    var extension = new Extension(extensionType, extensionContent);
    module.extensions.push(extension);
  }
  
  function loadComponents(app) {
    if (findModule(app)) {
      return findModule(app).components;
    }
    return [];
  }
  
  function loadExtensions(app, extensionType) {
    var extensions = [];
    var module = findModule(app);
    
    if (module && module.extensions.length > 0 ) {
      module.extensions.forEach(function(extension) {
        if (extension.extensionType === extensionType) {
          extensions.push(extension.extensionContent);
        }
      });
    }
  
    return extensions;
  }

  function findModule(module, create) {
    var foundModule = registry.find(function(element) {
      return element.moduleName === module;
    });
    
    if (!foundModule && create) {
      foundModule = new Module(module);
      registry.push(foundModule);
    }
    
    return foundModule;
  }
  
  var extensionRegistry = {
    "registerComponent": registerComponent, 
    "loadComponents": loadComponents,
    "registerExtension": registerExtension, 
    "loadExtensions": loadExtensions
  };
  
  // export for nodejs (for tests)
  if (typeof exports === 'object' && typeof module !== 'undefined') {
    module.exports = extensionRegistry
  } 

  return extensionRegistry;
  
})();
