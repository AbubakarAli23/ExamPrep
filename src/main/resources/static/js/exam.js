(function () {
  "use strict";

  var timer = document.getElementById("exam-timer");
  var form = document.getElementById("exam-form");
  if (!timer || !form) {
    return;
  }

  var deadline = parseInt(timer.getAttribute("data-deadline"), 10);
  if (!deadline || isNaN(deadline)) {
    timer.textContent = "—";
    return;
  }

  var submitting = false;

  function pad(value) {
    return value < 10 ? "0" + value : String(value);
  }

  function tick() {
    var remaining = deadline - Date.now();
    if (remaining <= 0) {
      timer.textContent = "00:00";
      timer.classList.add("is-urgent");
      if (!submitting) {
        submitting = true;
        form.submit();
      }
      return;
    }
    var totalSeconds = Math.floor(remaining / 1000);
    var hours = Math.floor(totalSeconds / 3600);
    var minutes = Math.floor((totalSeconds % 3600) / 60);
    var seconds = totalSeconds % 60;
    timer.textContent = (hours > 0 ? hours + ":" : "") + pad(minutes) + ":" + pad(seconds);
    timer.classList.toggle("is-urgent", totalSeconds <= 300);
    window.setTimeout(tick, 500);
  }

  form.addEventListener("submit", function (event) {
    if (submitting) {
      return;
    }
    var unanswered = 0;
    var questions = form.querySelectorAll(".exam-question");
    questions.forEach(function (question) {
      var inputs = question.querySelectorAll("input[type=radio]:checked, textarea");
      var answered = false;
      if (inputs.length > 0) {
        inputs.forEach(function (input) {
          if (input.type === "radio" || (input.value && input.value.trim() !== "")) {
            answered = true;
          }
        });
      }
      if (!answered) {
        unanswered += 1;
      }
    });
    var message = "Submit the examination now? You cannot change your answers afterwards.";
    if (unanswered > 0) {
      message = unanswered + " question(s) are unanswered. " + message;
    }
    if (!window.confirm(message)) {
      event.preventDefault();
      return;
    }
    submitting = true;
  });

  tick();
})();
