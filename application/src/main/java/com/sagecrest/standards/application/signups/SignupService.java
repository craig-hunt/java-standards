package com.sagecrest.standards.application.signups;

import com.sagecrest.standards.application.ports.SignupStore;
import com.sagecrest.standards.domain.signups.Signup;
import com.sagecrest.standards.domain.signups.SignupConfirmation;
import com.sagecrest.standards.domain.signups.SignupId;

/**
 * Records a signup that already passed validation.
 *
 * <p>Validation stays in the domain and runs before this call, so the service cannot receive a
 * signup with a missing email or an unknown plan. The type system says so: a {@link Signup} cannot
 * be built out of an invalid request.
 */
public final class SignupService {

  private final SignupStore store;

  public SignupService(SignupStore store) {
    this.store = store;
  }

  public SignupConfirmation create(Signup signup) {
    SignupId id = store.save(signup);
    return new SignupConfirmation(id, signup.summary());
  }
}
