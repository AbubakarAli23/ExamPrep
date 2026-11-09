(function () {
  "use strict";

  var panel = document.getElementById("generation-status");
  if (!panel) {
    return;
  }

  var studyPackId = panel.getAttribute("data-study-pack-id");
  var state = panel.getAttribute("data-status");
  var bar = document.getElementById("generation-bar");
  var message = document.getElementById("generation-message");
  var percent = document.getElementById("generation-percent");

  if (!studyPackId || (state !== "QUEUED" && state !== "PROCESSING")) {
    return;
  }

  var poll = function () {
    fetch("/study/" + encodeURIComponent(studyPackId) + "/status", {
      headers: { "Accept": "application/json" }
    }).then(function (response) {
      if (!response.ok) {
        throw new Error("status " + response.status);
      }
      return response.json();
    }).then(function (payload) {
      if (bar) {
        bar.style.width = (payload.progressPercent || 15) + "%";
      }
      if (message) {
        message.textContent = payload.message || "Working on your study package.";
      }
      if (percent) {
        percent.textContent = payload.status === "READY" ? "Ready" : "Working";
      }
      if (payload.status === "READY" || payload.status === "FAILED") {
        window.setTimeout(function () {
          window.location.reload();
        }, 600);
        return;
      }
      window.setTimeout(poll, 4000);
    }).catch(function () {
      window.setTimeout(poll, 7000);
    });
  };

  window.setTimeout(poll, 2500);
})();
