(function () {
  "use strict";

  var deck = document.getElementById("flashcard-deck");
  if (!deck) {
    return;
  }

  var slides = Array.prototype.slice.call(deck.querySelectorAll(".flashcard-slide"));
  if (slides.length === 0) {
    return;
  }

  deck.classList.add("js-deck");

  var index = 0;
  var revealed = false;
  var counter = document.getElementById("flashcard-counter");
  var status = document.getElementById("flashcard-status");
  var revealButton = document.getElementById("flashcard-reveal");

  function render() {
    slides.forEach(function (slide, slideIndex) {
      slide.classList.toggle("is-active", slideIndex === index);
      slide.classList.toggle("is-revealed", slideIndex === index && revealed);
      slide.setAttribute("aria-hidden", slideIndex === index ? "false" : "true");
    });
    if (counter) {
      counter.textContent = (index + 1) + " / " + slides.length;
    }
    if (revealButton) {
      revealButton.textContent = revealed ? "Hide answer" : "Reveal answer";
    }
    if (status) {
      status.textContent = revealed
        ? "Answer revealed. Move to the next card when you are ready."
        : "Try to recall the answer, then reveal it.";
    }
  }

  function next() {
    if (index < slides.length - 1) {
      index += 1;
      revealed = false;
      render();
    } else if (status) {
      status.textContent = "That was the last card. Record the review to add it to your study progress.";
    }
  }

  function previous() {
    if (index > 0) {
      index -= 1;
      revealed = false;
      render();
    }
  }

  if (revealButton) {
    revealButton.addEventListener("click", function () {
      revealed = !revealed;
      render();
    });
  }

  var nextButton = document.getElementById("flashcard-next");
  if (nextButton) {
    nextButton.addEventListener("click", next);
  }

  var prevButton = document.getElementById("flashcard-prev");
  if (prevButton) {
    prevButton.addEventListener("click", previous);
  }

  document.addEventListener("keydown", function (event) {
    if (event.target && /INPUT|TEXTAREA|SELECT/.test(event.target.tagName)) {
      return;
    }
    if (event.key === "ArrowRight") {
      next();
    } else if (event.key === "ArrowLeft") {
      previous();
    } else if (event.key === " " || event.key === "Enter") {
      if (event.target === document.body) {
        event.preventDefault();
        revealed = !revealed;
        render();
      }
    }
  });

  render();
})();
