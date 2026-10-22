import React from 'react';
import renderer from 'react-test-renderer';

import {LoginScreen} from '../src/screens/LoginScreen';
import {RegisterScreen} from '../src/screens/RegisterScreen';

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
      <RegisterScreen
        navigation={{navigate: jest.fn()} as any}
        route={{key: 'Register', name: 'Register'} as any}
      />,
    );
    expect(tree.toJSON()).toBeTruthy();
  });
});
