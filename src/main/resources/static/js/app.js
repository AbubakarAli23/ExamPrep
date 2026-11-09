(function () {
  "use strict";

  // Mobile navigation ------------------------------------------------------
  document.querySelectorAll("[data-nav-toggle]").forEach(function (button) {
    button.addEventListener("click", function () {
      var nav = document.getElementById(button.getAttribute("aria-controls"));
      if (!nav) {
        return;
      }
      var open = nav.classList.toggle("is-open");
      button.setAttribute("aria-expanded", open ? "true" : "false");
    });
  });

  // Destructive actions ----------------------------------------------------
  document.querySelectorAll("form[data-confirm]").forEach(function (form) {
    form.addEventListener("submit", function (event) {
      var message = form.getAttribute("data-confirm");
      if (message && !window.confirm(message)) {
        event.preventDefault();
      }
    });
  });

  // CSRF helper shared with page scripts ----------------------------------
  window.examPrep = window.examPrep || {};
  window.examPrep.csrf = function () {
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    return {
      token: token ? token.getAttribute("content") : "",
      header: header ? header.getAttribute("content") : "X-CSRF-TOKEN"
    };
  };

  window.examPrep.postForm = function (url, body) {
    var csrf = window.examPrep.csrf();
    return fetch(url, {
      method: "POST",
      headers: (function () {
        var headers = {
          "Content-Type": "application/x-www-form-urlencoded",
          "X-Requested-With": "fetch"
        };
        if (csrf.token) {
          headers[csrf.header] = csrf.token;
        }
        return headers;
      })(),
      body: body || ""
    });
  };
})();
