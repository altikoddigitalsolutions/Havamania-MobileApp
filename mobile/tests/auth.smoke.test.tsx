import React from 'react';
import renderer from 'react-test-renderer';

import {LoginScreen} from '../src/screens/LoginScreen';
import {SignUpScreen} from '../src/screens/SignUpScreen';

describe('Auth screens', () => {
  it('renders login screen', () => {
    const tree = renderer.create(
      <LoginScreen
        navigation={{navigate: jest.fn()} as any}
        route={{key: 'Login', name: 'Login'} as any}
      />,
    );
    expect(tree.toJSON()).toBeTruthy();
  });

  it('renders signup screen', () => {
    const tree = renderer.create(
      <SignUpScreen
        navigation={{navigate: jest.fn()} as any}
        route={{key: 'SignUp', name: 'SignUp'} as any}
      />,
    );
    expect(tree.toJSON()).toBeTruthy();
  });
});
