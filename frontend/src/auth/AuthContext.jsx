import { useEffect, useMemo, useState } from "react";
import * as authService from "../services/authService.js";
import { AuthContext } from "./authContext.js";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    authService
      .getCurrentUser()
      .then((currentUser) => {
        if (active) setUser(currentUser);
      })
      .catch(() => {
        if (active) setUser(null);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const value = useMemo(
    () => ({
      user,
      loading,
      async refresh() {
        const currentUser = await authService.getCurrentUser();
        setUser(currentUser);
        return currentUser;
      },
      async login(credentials) {
        const authenticatedUser = await authService.login(credentials);
        setUser(authenticatedUser);
        return authenticatedUser;
      },
      async register(details) {
        return authService.register(details);
      },
      async verifyRegistration(details) {
        const verifiedUser = await authService.verifyRegistration(details);
        setUser(verifiedUser);
        return verifiedUser;
      },
      async logout() {
        await authService.logout();
        setUser(null);
      },
    }),
    [user, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
