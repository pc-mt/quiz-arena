// --- Objekt-CRUD: Create ---
document
  .getElementById("create-form")
  .addEventListener("submit", function (event) {
    event.preventDefault();
    const message = document.getElementById("create-message").value;

    fetch("/api/objects", {
      method: "POST",
      headers: {
        "Content-Type": "text/plain",
      },
      body: message,
    })
      .then((response) => response.text())
      .then((data) => alert(data))
      .catch((error) => console.error("Error:", error));
  });

// --- Objekt-CRUD: Update ---
document
  .getElementById("update-form")
  .addEventListener("submit", function (event) {
    event.preventDefault();
    const id = document.getElementById("update-id").value;
    const message = document.getElementById("update-message").value;

    fetch(`/api/objects/${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "text/plain",
      },
      body: message,
    })
      .then((response) => response.text())
      .then((data) => alert(data))
      .catch((error) => console.error("Error:", error));
  });

// --- Objekt-CRUD: Delete ---
document
  .getElementById("delete-form")
  .addEventListener("submit", function (event) {
    event.preventDefault();
    const id = document.getElementById("delete-id").value;

    fetch(`/api/objects/${id}`, {
      method: "DELETE",
    })
      .then((response) => response.text())
      .then((data) => alert(data))
      .catch((error) => console.error("Error:", error));
  });

// --- Objekt-CRUD: Read ---
document
  .getElementById("fetch-objects")
  .addEventListener("click", function () {
    fetch("/api/objects")
      .then((response) => response.json())
      .then((data) => {
        const objectsList = document.getElementById("objects-list");
        objectsList.innerHTML = "";
        data.forEach((object) => {
          const listItem = document.createElement("li");
          listItem.className = "list-group-item";
          listItem.textContent = `ID: ${object.id}, Message: ${object.message}, Created At: ${object.created_at}`;
          objectsList.appendChild(listItem);
        });
      })
      .catch((error) => console.error("Error:", error));
  });

// --- Webcontroller-Test ---
document
  .getElementById("open-webcontroller")
  .addEventListener("click", function () {
      open("http://localhost:81", "_blank", "popup,width=450,height=700")
  });
