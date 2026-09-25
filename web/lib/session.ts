/**
 * There is no real auth session wired to the frontend yet (the backend auth module and
 * the frontend auth wiring are both still unbuilt). Every page that needs a "current user"
 * imports this one placeholder instead of hardcoding its own copy, so there is a single
 * place to swap in the real session lookup once it exists.
 */
export const mockCurrentUser = {
  firstName: "Caleb",
  lastName: "Nzabanita",
  email: "cnzabb@gmail.com",
};
