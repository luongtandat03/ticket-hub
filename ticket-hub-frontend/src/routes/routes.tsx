import { createBrowserRouter } from 'react-router-dom';
import Layout from '../components/Layout';
import HomePage from '../pages/HomePage';
import EventsPage from '../pages/EventsPage';
import AboutPage from '../pages/AboutPage';
import ContactPage from '../pages/ContactPage';
import ManagerPage from '../pages/ManagerPage';
import NotFound from '../pages/NotFound';

const routes = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'concerts',
        element: <EventsPage />,
      },
      {
        path: 'singers',
        element: <AboutPage />,
      },
      {
        path: 'cart',
        element: <EventsPage />,
      },
      {
        path: 'login',
        element: <AboutPage />,
      },
      {
        path: 'register',
        element: <AboutPage />,
      },
      {
        path: 'about',
        element: <AboutPage />,
      },
      {
        path: 'contact',
        element: <ContactPage />,
      },
      {
        path: 'manager',
        element: <ManagerPage />,
      },
      {
        path: '*',
        element: <NotFound />,
      },
    ],
  },
]);

export default routes;