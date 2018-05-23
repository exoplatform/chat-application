

function ExtCometD(origin) {
  this.parent = origin;

  $.extend(this, {
    eXoSecret: {exoId: null, exoToken: null},
    eXoResubs: [],
    eXoPublish: [],
    eXoRemoteCalls: [],
    autoResubscribe: true,
    explicitlyDisconnected: false
  }, origin);

  this.configure = function(config) {
    this.isConfigured = true;

    if (config.exoId) {
      this.eXoSecret = {
        exoId: config.exoId,
        exoToken: config.exoToken
      };
    }
    if (typeof config.autoResubscribe !== 'undefined') {
      this.autoResubscribe = config.autoResubscribe;
    }
    this.parent.configure.apply(this, config);
  };

  this.subscribe = function(channel, scope, callback, subscribeProps, subscribeCallback) {
    // Normalize arguments
    if ($.isFunction(scope)) {
      subscribeCallback = subscribeProps;
      subscribeProps = callback;
      callback = scope;
      scope = null;
    }
    if ($.isFunction(subscribeProps)) {
      subscribeCallback = subscribeProps;
      subscribeProps = null;
    }

    //Add eXo token
    if (!subscribeProps) {
      subscribeProps = {};
    }
    subscribeProps = $.extend({}, this.eXoSecret, subscribeProps);

    if (this.autoResubscribe) {
      this.eXoResubs.push([channel, scope, callback, subscribeProps, subscribeCallback]);
    }

    if (this.isDisconnected()) {
      if(!this.explicitlyDisconnected) {
        this.handshake(subscribeProps);
      }
    } else if(this.getStatus() !== 'handshaking') {
      return this.parent.subscribe.call(this, channel, scope, callback, subscribeProps, subscribeCallback);
    }
  };

  this.publish = function(channel, content, publishProps, publishCallback) {
    if (this.isDisconnected()) {
      if(!this.explicitlyDisconnected) {
        if (!publishProps || $.isFunction(publishProps)) {
          publishProps = {};
        }

        //Add eXo token
        publishProps = $.extend({}, this.eXoSecret, publishProps);
        this.handshake(publishProps);
      }
    } else if(this.getStatus() === 'handshaking') {
      this.eXoPublish.push(channel, content, publishProps, publishCallback);
    } else {
      return this.parent.publish.call(this, channel, content, publishProps, publishCallback);
    }
  };

  this.remoteCall = function(target, content, timeout, callback) {
    if (this.isDisconnected()) {
      if(!this.explicitlyDisconnected) {
        if (!content || $.isFunction(content)) {
          content = {};
        }

        //Add eXo token
        content = $.extend({}, this.eXoSecret, content);
        this.handshake(content);
      }
    } else if(this.getStatus() === 'handshaking') {
      this.eXoRemoteCalls.push(target, content, timeout, callback);
    } else {
      return this.parent.remoteCall.call(this, target, content, timeout, callback);
    }
  };

  this.clearResubscriptions = function() {
    this.eXoResubs = [];
  };

  this.disconnect = function(...args) {
    this.eXoSecret = {exoId: null, exoToken: null};
    this.explicitlyDisconnected = true;
    this.parent.disconnect.apply(this, args);
  };
}

const cCometD = new ExtCometD($.cometd);

cCometD.getInstance = function(name) {
  if (name) {
    if (!this.instances) {
      this.instances = {};
    }

    if (!this.instances[name]) {
      this.instances[name] = new ExtCometD(new $.CometD());
    }

    return this.instances[name];
  } else {
    return this;
  }
};

export { cCometD as cCometD };
