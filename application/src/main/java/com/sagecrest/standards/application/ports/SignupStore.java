package com.sagecrest.standards.application.ports;

import com.sagecrest.standards.domain.signups.Signup;
import com.sagecrest.standards.domain.signups.SignupId;

/** Records a validated signup and answers with the identifier it received. */
public interface SignupStore {

  SignupId save(Signup signup);
}
