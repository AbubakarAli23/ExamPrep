(function () {
  "use strict";

  var form = document.getElementById("upload-form");
  var zone = document.getElementById("upload-zone");
  var fileInput = document.getElementById("document-file");
  var selected = document.getElementById("selected-file");
  var progress = document.getElementById("upload-progress");
  var bar = document.getElementById("upload-bar-fill");
  var status = document.getElementById("upload-status");
  var submit = document.getElementById("upload-submit");
  var maxBytes = null;

  if (!form || !fileInput || !window.fetch || !window.FormData) {
    return;
  }

  function humanSize(bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    }
    if (bytes < 1024 * 1024) {
      return Math.round(bytes / 1024) + " KB";
    }
    return (bytes / (1024 * 1024)).toFixed(1) + " MB";
  }

  function setStatus(message, failed) {
    if (!status) {
      return;
    }
    status.textContent = message;
    status.style.color = failed ? "var(--danger)" : "";
  }

  fileInput.addEventListener("change", function () {
    if (fileInput.files && fileInput.files.length > 0) {
      selected.textContent = "Selected: " + fileInput.files[0].name + " (" + humanSize(fileInput.files[0].size) + ")";
    } else {
      selected.textContent = "";
    }
  });

  ["dragenter", "dragover"].forEach(function (eventName) {
    zone.addEventListener(eventName, function (event) {
      event.preventDefault();
      zone.classList.add("is-dragging");
    });
  });

  ["dragleave", "drop"].forEach(function (eventName) {
    zone.addEventListener(eventName, function (event) {
      event.preventDefault();
      zone.classList.remove("is-dragging");
    });
  });

  zone.addEventListener("drop", function (event) {
    if (!event.dataTransfer || !event.dataTransfer.files.length) {
      return;
    }
    fileInput.files = event.dataTransfer.files;
    fileInput.dispatchEvent(new Event("change"));
  });

  form.addEventListener("submit", function (event) {
    if (!fileInput.files || fileInput.files.length === 0) {
      return;
    }
    event.preventDefault();

    var file = fileInput.files[0];
    var data = new FormData();
    data.append("file", file);

    progress.hidden = false;
    submit.disabled = true;
    bar.style.width = "4%";
    setStatus("Uploading " + file.name + "…", false);

    var request = new XMLHttpRequest();
    request.open("POST", "/documents/upload-json");
    var csrf = window.examPrep.csrf();
    if (csrf.token) {
      request.setRequestHeader(csrf.header, csrf.token);
    }
    request.upload.addEventListener("progress", function (progressEvent) {
      if (progressEvent.lengthComputable) {
        var percent = Math.max(4, Math.round((progressEvent.loaded / progressEvent.total) * 100));
        bar.style.width = percent + "%";
      }
    });
    request.addEventListener("load", function () {
      bar.style.width = "100%";
      if (request.status >= 200 && request.status < 300) {
        setStatus("Upload received. Reading the document…", false);
        window.setTimeout(function () {
          window.location.reload();
        }, 700);
      } else {
        var message = "The document could not be uploaded.";
        try {
          var payload = JSON.parse(request.responseText);
          if (payload && payload.error) {
            message = payload.error;
          }
        } catch (ignored) {
          message = "The server returned an unexpected response (" + request.status + ").";
        }
        setStatus(message, true);
        submit.disabled = false;
        bar.style.width = "0";
      }
    });
    request.addEventListener("error", function () {
      setStatus("The upload failed because the connection was interrupted.", true);
      submit.disabled = false;
    });
    request.send(data);
  });

  // Poll extraction status for documents still being processed -------------
  var rows = document.querySelectorAll("[data-document-id][data-document-status]");
  rows.forEach(function (row) {
    var documentId = row.getAttribute("data-document-id");
    var state = row.getAttribute("data-document-status");
    if (state !== "UPLOADED" && state !== "PROCESSING") {
      return;
    }
    var pill = row.querySelector(".document-status");
    var message = row.querySelector(".document-status-message");

    var poll = function () {
      fetch("/documents/" + encodeURIComponent(documentId) + "/status", {
        headers: { "Accept": "application/json" }
      }).then(function (response) {
        if (!response.ok) {
          throw new Error("status " + response.status);
        }
        return response.json();
      }).then(function (payload) {
        if (pill) {
          pill.textContent = payload.status;
          pill.classList.remove("pill--ready", "pill--failed", "pill--busy");
          pill.classList.add(payload.status === "READY" ? "pill--ready" : (payload.status === "FAILED" ? "pill--failed" : "pill--busy"));
        }
        if (message) {
          message.textContent = payload.message || "";
        }
        if (payload.status === "READY" || payload.status === "FAILED") {
          window.setTimeout(function () {
            window.location.reload();
          }, 600);
          return;
        }
        window.setTimeout(poll, 3000);
      }).catch(function () {
        window.setTimeout(poll, 5000);
      });
    };
    window.setTimeout(poll, 1500);
  });
})();
