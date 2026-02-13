import React from 'react';
import renderer from 'react-test-renderer';

import {AlertsScreen} from '../src/screens/AlertsScreen';
import {HomeScreen} from '../src/screens/HomeScreen';
import {MapScreen} from '../src/screens/MapScreen';
import {SettingsScreen} from '../src/screens/SettingsScreen';

describe('Main screens', () => {
  it('renders home', () => {
    expect(renderer.create(<HomeScreen />).toJSON()).toBeTruthy();
  });

  it('renders map', () => {
    expect(renderer.create(<MapScreen />).toJSON()).toBeTruthy();
  });

  it('renders alerts', () => {
    expect(renderer.create(<AlertsScreen />).toJSON()).toBeTruthy();
  });

  it('renders settings', () => {
    expect(
      renderer.create(
        <SettingsScreen
          navigation={{navigate: jest.fn()} as any}
          route={{key: 'SettingsHome', name: 'SettingsHome'} as any}
        />,
      ).toJSON(),
    ).toBeTruthy();
  });
});
