import React from 'react';
import {Text, View} from 'react-native';

type Props = {
  children: React.ReactNode;
};

type State = {
  hasError: boolean;
};

export class ErrorBoundary extends React.Component<Props, State> {
  public constructor(props: Props) {
    super(props);
    this.state = {hasError: false};
  }

  public static getDerivedStateFromError(): State {
    return {hasError: true};
  }

  public componentDidCatch(error: Error): void {
    console.error('Unhandled UI error', error);
  }

  public render(): React.ReactNode {
    if (this.state.hasError) {
      return (
        <View style={{flex: 1, justifyContent: 'center', alignItems: 'center'}}>
          <Text>Beklenmeyen bir hata olustu.</Text>
        </View>
      );
    }

    return this.props.children;
  }
}
