import React from 'react';
import { render } from 'react-dom';
import CssBaseline from '@mui/material/CssBaseline';

import { App } from './App';
import { RootStore, RootStoreProvider } from './RootStore';
import { ThemeProvider } from './theme/ThemeProvider';

import '../css/index.scss';

const initialValue = `class Family {
  contains Person[] members
}

class Person {
  Person[] children opposite parent
  Person[0..1] parent opposite children
  int age
  TaxStatus taxStatus
}

enum TaxStatus {
  child, student, adult, retired
}

% A child cannot have any dependents.
error invalidTaxStatus(Person p) <->
  taxStatus(p, child), children(p, _q).

unique family.
Family(family).
members(family, anne).
members(family, bob).
members(family, ciri).
children(anne, ciri).
?children(bob, ciri).
default children(ciri, *): false.
taxStatus(anne, adult).
age(anne, 35).
bobAge: 27.
age(bob, bobAge).
!age(ciri, bobAge).

scope Family = 1, Person += 5..10.
`;

const rootStore = new RootStore();
rootStore.editorStore.updateValue(initialValue);

const app = (
  <RootStoreProvider rootStore={rootStore}>
    <ThemeProvider>
      <CssBaseline />
      <App />
    </ThemeProvider>
  </RootStoreProvider>
);

render(app, document.getElementById('app'));