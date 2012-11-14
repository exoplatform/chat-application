  jzGetParam = function(key) {
    var ts  = localStorage.getItem(key+"TS");
    var val = localStorage.getItem(key);
    if (!ts) ts=-1;

    var now = Math.round(new Date()/1000);

    if (val !== undefined && (now<ts || ts===-1 )) {
      return val;
    }

    return undefined;
  };

  jzStoreParam = function(key, value, expire) {
    expire = typeof expire !== 'undefined' ? expire : 300;
    localStorage.setItem(key+"TS", Math.round(new Date()/1000) + expire);
    localStorage.setItem(key, value);
  };
