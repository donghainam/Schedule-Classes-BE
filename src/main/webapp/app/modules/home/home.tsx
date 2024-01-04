import './home.scss';

import React from 'react';
import { Link } from 'react-router-dom';

import { Row, Col, Alert } from 'reactstrap';

import { useAppSelector } from 'app/config/store';

export const Home = () => {
  const account = useAppSelector(state => state.authentication.account);

  return (
    <section className="welcome">
      <div className="welcomeMain">
        <Row>
          <Col md="8">
            <div className="heading-main-box">
              <h2>This is administration page!</h2>
              <h5>If you have any question about website:</h5>

              <ul>
                <li className="home-text">Developer email nam.dh183804@sis.hust.edu.vn</li>
                <li>
                  <a href="https://www.fb.com/namdh2119" target="_blank" rel="noopener noreferrer" className="home-text">
                    Developer facebook
                  </a>
                </li>
                <li>
                  <a href="https://www.instagram.com/namdh2119/" target="_blank" rel="noopener noreferrer" className="home-text">
                    Developer instagram
                  </a>
                </li>
                <li>
                  <a href="https://t.me/namdh183804" target="_blank" rel="noopener noreferrer" className="home-text">
                    Developer telegram
                  </a>
                </li>
                <li>
                  <a
                    href="https://www.youtube.com/channel/UCOZA4d1tRkkEZ3A7D5sFUQA"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="home-text"
                  >
                    Developer channel
                  </a>
                </li>
              </ul>
            </div>
          </Col>
        </Row>
      </div>
    </section>
  );
};

export default Home;
